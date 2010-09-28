package com.lasic.interpreter

import com.lasic.values.BaseAction
import java.net.URI
import com.lasic.model.{ScaleGroupInstance, NodeInstance, VMHolder, ScriptArgumentValue}
import concurrent.ops._
import java.io.File
import java.util.Date
import com.lasic.cloud.{ImageState, ScalingGroup, ScalingTrigger, LaunchConfiguration}
import java.lang.String
import com.lasic.LasicProperties
import com.lasic.util.Logging

protected class VMState() {
  var scpComplete = false
  var scriptsComplete = false
  var ipsComplete = false
}


class NodeIPState (val node: NodeInstance,val elasticIp: String,var pubDnsMatch: Boolean)

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
    spawn {
      allSCPs.foreach {
        tuple => vmHolder.vm.copyTo(build(new URI(tuple._1)), tuple._2)
      }
      vmState.synchronized(
        vmState(vmHolder).scpComplete = true
        )
      resolvedScripts.foreach {
        script =>
          val scriptName = script._1
          val argMap = script._2
          //vm.execute(scriptName)
          vmHolder.vm.executeScript(scriptName, argMap)
      }
      vmState.synchronized(
        vmState(vmHolder).scriptsComplete = true
        )
      allIPs.foreach {
        ip =>
          vmHolder match {
            case holder: NodeInstance => {
              if (holder.idx == ip._1) {
                holder.vm.associateAddressWith(ip._2)
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
  def getActionItemMaps(actionName: String, allActions: List[BaseAction]): (Map[String, Map[String, ScriptArgumentValue]], Map[String, String], Map[Int, String]) = {
    val deployActions = allActions.filter(_.name == actionName)
    var allSCPs = Map[String, String]()
    var allScripts = Map[String, Map[String, ScriptArgumentValue]]()
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


  def createImageForScaleGroup(scaleGroupInstance: ScaleGroupInstance, scaleGroup: ScalingGroup): String = {
    //create the image
    val desc = "Created by LASIC for scale group [" + scaleGroupInstance.cloudName + "] from instanceid [" + scaleGroupInstance.vm.instanceId + "]"
    var imageID = scaleGroup.createImageForScaleGroup(scaleGroupInstance.vm.instanceId, scaleGroupInstance.cloudName, desc, true)

    //wait for image to be available
    var imageState = ImageState.Unknown
    var numRetries = 0
    while (imageState != ImageState.Available) {
      Thread.sleep(sleepDelay)
      imageState = scaleGroup.getImageState(imageID)
      if (imageState == ImageState.Failed) {
        if (numRetries >= 1) {
          throw new Exception("Image creation failed for an unknown reason for imagedId [" + imageID + "] for scale group [" + scaleGroupInstance.cloudName + "]")
        }
        else {
          logger.warn("creation of  image [ " + imageID + "] for scale group [" + scaleGroupInstance.cloudName + "] failed for unknown reason.  Trying one more time in 1 minute...")
          Thread.sleep(60000)
          imageID = scaleGroup.createImageForScaleGroup(scaleGroupInstance.vm.instanceId, scaleGroupInstance.cloudName, desc, true)
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
  def createScaleGroups(scaleGroup: ScalingGroup) {
    scaleGroups.foreach {
      scaleGroupInstance =>
        spawn {
          var imageID = createImageForScaleGroup(scaleGroupInstance, scaleGroup)

          //create the config
          val scaleGroupConfig = scaleGroupInstance.configuration
          val launchConfiguration = LaunchConfiguration.build(scaleGroupInstance.configuration)
          launchConfiguration.machineImage = imageID
          launchConfiguration.name = scaleGroupConfig.cloudName
          scaleGroup.createScalingLaunchConfiguration(launchConfiguration)

          //create the group
          scaleGroup.createScalingGroup(scaleGroupInstance.cloudName, scaleGroupConfig.cloudName, scaleGroupConfig.minSize, scaleGroupConfig.maxSize, List(launchConfiguration.availabilityZone))

          //create the triggers
          scaleGroupInstance.triggers.foreach {
            trigger =>
              val scalingTrigger = new ScalingTrigger(scaleGroupInstance.cloudName,
                trigger.breachDuration,
                trigger.lowerBreachIncrement.toString,
                trigger.lowerThreshold,
                trigger.measure,
                trigger.name,
                trigger.namespace,
                trigger.period,
                trigger.upperBreachIncrement.toString,
                trigger.upperThreshold)
              scaleGroup.createUpdateScalingTrigger(scalingTrigger)
          }

          //terminate the original vm
          scaleGroupInstance.vm.shutdown
        }
    }

  }


  private def build(uri: URI): File = {
    if (uri.isOpaque) new File(uri.toString.split(":")(1)) else new File(uri)
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
    var waiting = ipNodeMap
    val startTime = System.currentTimeMillis
    while (!waiting.isEmpty && !isTimedOut(startTime, maxWaitSeconds) ) {
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
      waiting = ipNodeMap.filter(nm => !nm.pubDnsMatch)
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