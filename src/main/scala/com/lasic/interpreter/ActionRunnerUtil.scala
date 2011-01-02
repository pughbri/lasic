package com.lasic.interpreter

import com.lasic.values.BaseAction
import com.lasic.concurrent.ops._
import java.util.Date
import com.lasic.cloud.{ImageState, ScalingGroupClient, ScalingTrigger, LaunchConfiguration}
import java.lang.String
import com.lasic.LasicProperties
import com.lasic.util.Logging
import com.lasic.model._
import java.io.{FileOutputStream, File}
import java.net.{URLEncoder, URI}

protected class VMState() {
  var scpComplete = false
  var scriptsComplete = false
  var ipsComplete = false
}


class NodeIPState(val node: NodeInstance, val elasticIp: String, var pubDnsMatch: Boolean)

/**
 *
 * Utility for "running actions" that are useful for various verbs.
 * @author Brian Pugh
 */
trait ActionRunnerUtil extends Logging {
  protected val vmState: Map[VMHolder, VMState]
  protected val nodes: List[NodeInstance]
  protected val scaleGroups: List[ScaleGroupInstance]

  private val sleepDelay = LasicProperties.getProperty("SLEEP_DELAY", "10000").toInt

  /**
   * Spawns a thread in which all the scp, script and ip statements are run.  vmStat is updated as appropriate.
   */
  def runActionItems(vmHolder: VMHolder, allSCPs: Map[String, String], resolvedScripts: Map[String, Map[String, scala.List[String]]], allIPs: Map[Int, String]) {
    spawn("run action items") {
      allSCPs.foreach {
        tuple => {
          if (vmHolder.vm != null) {
            vmHolder.vm.copyTo(build(new URI(tuple._1)), tuple._2)
          }
        }
      }
      vmState.synchronized {
        vmState(vmHolder).scpComplete = true
      }

      resolvedScripts.foreach {
        script =>
          val scriptName = script._1
          val argMap = script._2
          //vm.execute(scriptName)
          if (vmHolder.vm != null) {
            vmHolder.vm.executeScript(scriptName, argMap)
          }
      }
      vmState.synchronized(
        vmState(vmHolder).scriptsComplete = true
        )
      allIPs.foreach {
        ip =>
          vmHolder match {
            case holder: NodeInstance => {
              if (holder.idx == ip._1) {
                if (holder.vm != null) {
                  holder.vm.associateAddressWith(ip._2)
                }
              }
            }
            case unknown => throw new IllegalStateException("unkown type of vmholder: cannot assign elastic ip: " + unknown.getClass)
          }
      }
      vmState.synchronized(
        vmState(vmHolder).ipsComplete = true
        )

    }
  }

  /**
   * Find all actions mapping action name and return a tuple contain the scripts, scps and ips.
   */
  def getActionItemMaps(actionName: String, allActions: List[BaseAction]): (Map[String, Map[String, ArgumentValue]], Map[String, String], Map[Int, String]) = {
    val deployActions = allActions.filter(_.name == actionName)
    var allSCPs = Map[String, String]()
    var allScripts = Map[String, Map[String, ArgumentValue]]()
    var allIPs = Map[Int, String]()

    deployActions.foreach {
      action => {
        allSCPs = allSCPs ++ action.scpMap
        allScripts = allScripts ++ action.scriptMap
        allIPs = allIPs ++ action.ipMap
      }
    }
    (allScripts, allSCPs, allIPs)
  }

  /**
   * Run actions statements matching name.
   */
  def startAsyncRunAction(actionName: String) {
    nodes.foreach {
      node =>
        val allActions = node.parent.actions
        val (allScripts, allSCPs, allIPs) = getActionItemMaps(actionName, allActions)
        var resolvedScripts = node.resolveScripts(allScripts)
        runActionItems(node, allSCPs, resolvedScripts, allIPs)
    }

    scaleGroups.foreach {
      scaleGroup =>
        val allActions = scaleGroup.actions
        val (allScripts, allSCPs, allIPs) = getActionItemMaps(actionName, allActions)
        var resolvedScripts = scaleGroup.resolveScripts(allScripts)
        runActionItems(scaleGroup, allSCPs, resolvedScripts, allIPs)
    }
  }


  def setScaleGroupNames {
    scaleGroups foreach {
      scaleGroupInstance =>
      //create unique names
        val dateString = new java.text.SimpleDateFormat("yyyy-MM-dd-HH-mm-ss").format(new Date())
        scaleGroupInstance.cloudName = scaleGroupInstance.localName + "-" + dateString
        scaleGroupInstance.configuration.cloudName = scaleGroupInstance.configuration.name + "-" + dateString
    }
  }


  def createImageForScaleGroup(scaleGroupInstance: ScaleGroupInstance, scalingGroupClient: ScalingGroupClient): String = {
    //create the image
    val desc = "Created by LASIC for scale group [" + scaleGroupInstance.cloudName + "] from instanceid [" + scaleGroupInstance.vm.instanceId + "]"
    var imageID = scalingGroupClient.createImageForScaleGroup(scaleGroupInstance.vm.instanceId, scaleGroupInstance.cloudName, desc, true)

    //wait for image to be available
    var imageState = ImageState.Unknown
    var numRetries = 0
    while (imageState != ImageState.Available) {
      Thread.sleep(sleepDelay)
      imageState = scalingGroupClient.getImageState(imageID)
      if (imageState == ImageState.Failed) {
        if (numRetries >= 1) {
          throw new Exception("Image creation failed for an unknown reason for imagedId [" + imageID + "] for scale group [" + scaleGroupInstance.cloudName + "]")
        }
        else {
          logger.warn("creation of  image [ " + imageID + "] for scale group [" + scaleGroupInstance.cloudName + "] failed for unknown reason.  Trying one more time in 1 minute...")
          Thread.sleep(60000)
          imageID = scalingGroupClient.createImageForScaleGroup(scaleGroupInstance.vm.instanceId, scaleGroupInstance.cloudName, desc, true)
          imageState = ImageState.Unknown
          numRetries = numRetries + 1
        }
      }
    }
    imageID
  }

  /**
   * create scale groups based on the "prototype vm" on each scaleGroupInstance.  Once the scale group is created,
   * the prototype VM will be shutdown.
   */
  def createScaleGroups(scalingGroupClient: ScalingGroupClient) {
    scaleGroups.foreach {
      scaleGroupInst =>
        spawn("create scale groups") {
          var imageID = createImageForScaleGroup(scaleGroupInst, scalingGroupClient)

          //create the config
          val scaleGroupConfig = scaleGroupInst.configuration
          val launchConfiguration = LaunchConfiguration.build(scaleGroupInst.configuration)
          launchConfiguration.machineImage = imageID
          launchConfiguration.name = scaleGroupConfig.cloudName
          scalingGroupClient.createScalingLaunchConfiguration(launchConfiguration)

          //create the group
          val loadBalancers = scaleGroupInst.loadBalancers map (ScriptResolver.resolveArgumentValue(scaleGroupInst, _))
          scalingGroupClient.createScalingGroup(scaleGroupInst.cloudName,
            scaleGroupConfig.cloudName,
            scaleGroupConfig.minSize,
            scaleGroupConfig.maxSize,
            loadBalancers,
            List(launchConfiguration.availabilityZone))

          //create the triggers
          scaleGroupInst.triggers.foreach {
            trigger =>
              val scalingTrigger = new ScalingTrigger(scaleGroupInst.cloudName,
                trigger.breachDuration,
                trigger.lowerBreachIncrement.toString,
                trigger.lowerThreshold,
                trigger.measure,
                trigger.name,
                trigger.namespace,
                trigger.period,
                trigger.upperBreachIncrement.toString,
                trigger.upperThreshold)
              scalingGroupClient.createUpdateScalingTrigger(scalingTrigger)
          }

          //terminate the original vm
          scaleGroupInst.vm.shutdown
        }
    }

  }

  private def build(uri: URI): File = {
    uri.getScheme match {
      case "file" => convertFileURIToFile(uri)
      case "http" => convertHttpURIToFile(uri)
    }
  }

  def convertFileURIToFile(uri: URI): File = {
    if (uri.isOpaque)
      new File(uri.toString.split(":")(1))
    else
      new File(uri)
  }

  def convertHttpURIToFile(uri: URI): File = {
    val is = uri.toURL.openStream()
    val tmpFile = System.getProperty("java.io.tmpdir") + "/" +  URLEncoder.encode(uri.toString, "UTF-8")
    val fos = new FileOutputStream(tmpFile)
    var oneChar = is.read()
    while (oneChar != -1) {
      fos.write(oneChar)
      oneChar = is.read()
    }
    fos.close()
    is.close()
    new File(tmpFile)
  }


  def waitForElasticIpDnsChange(actionName: String) {
    var ipNodeMap = List[NodeIPState]()
    nodes.foreach({
      node =>
        val allIps = getNodeIps(node, actionName)
        allIps.foreach({
          ip =>
            if (node.idx == ip._1 && ip._2 != null && ip._2 != "") {
              ipNodeMap = new NodeIPState(node, ip._2, false) :: ipNodeMap
            }
        })
    })
    waitForElasticIpDnsChange(ipNodeMap, 120)
  }

  def isTimedOut(startTime: Long, maxWaitSeconds: Int): Boolean = {
    (((System.currentTimeMillis - startTime) / 1000) > maxWaitSeconds)
  }

  private def waitForElasticIpDnsChange(ipNodeMap: List[NodeIPState], maxWaitSeconds: Int) {
    var waiting = ipNodeMap filter (_.node.vm != null)
    val startTime = System.currentTimeMillis
    while (!waiting.isEmpty && !isTimedOut(startTime, maxWaitSeconds)) {
      ipNodeMap.foreach({
        nodeIpState =>
          if (!nodeIpState.pubDnsMatch && nodeIpState.node.vm.getPublicIpAddress != nodeIpState.elasticIp) {
            logger.info("Waiting for publicDns: " + nodeIpState.node.vm.getPublicDns() + " to match elastic ip: " + nodeIpState.elasticIp)
            Thread.sleep(sleepDelay)
          }
          else {
            nodeIpState.pubDnsMatch = true
          }
      })
      waiting = waiting filter (!_.pubDnsMatch)
    }
    if (!waiting.isEmpty) {
      logger.info("Timed out waiting for publicDns to be set for elastic ips after waiting " +
              maxWaitSeconds + " seconds.  DNS entries may not reflect the new public DNS name for " +
              waiting.map(_.node.vmId).mkString(", "))
    }
  }

  private def getNodeIps(node: NodeInstance, actionName: String): Map[Int, String] = {
    val allActions = node.parent.actions
    val deployActions = allActions.filter(_.name == actionName)
    var allIPs = Map[Int, String]()
    deployActions.foreach {
      action => {
        allIPs = allIPs ++ action.ipMap
      }
    }
    allIPs
  }
}