package com.lasic.interpreter

import com.lasic.values.BaseAction
import java.net.URI
import com.lasic.model.{ScaleGroupInstance, NodeInstance, VMHolder, ScriptArgumentValue}
import se.scalablesolutions.akka.actor.Actor._
import java.io.File
import java.util.Date
import com.lasic.cloud.{ImageState, ScalingGroup, ScalingTrigger, LaunchConfiguration}

protected class VMState() {
  var scpComplete = false
  var scriptsComplete = false
  var ipsComplete = false
}


/**
 *
 * Utility for "running actions" that are useful for various verbs.
 * @author Brian Pugh
 */
trait ActionRunnerUtil {
  protected val vmState: Map[VMHolder, VMState]
  protected val nodes: List[NodeInstance]
  protected val scaleGroups: List[ScaleGroupInstance]

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
        ip => vmHolder.vm.associateAddressWith(ip._2)
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

  /**
   * create scale groups based on the "prototype vm" on each scaleGroupInstance.  Once the scale group is created,
   * the prototype VM will be shutdown.
   */
  def createScaleGroups(scaleGroup: ScalingGroup) {
      scaleGroups.foreach {
        scaleGroupInstance =>
          spawn {

            //create unique names
            val dateString = new java.text.SimpleDateFormat("yyyy-MM-dd-HH-mm-ss").format(new Date())
            scaleGroupInstance.cloudName = scaleGroupInstance.localName + "-" + dateString
            val scaleGroupConfig = scaleGroupInstance.configuration
            scaleGroupConfig.cloudName = scaleGroupConfig.name + "-" + dateString

            //create the image
            val imageID = scaleGroup.createImageForScaleGroup(scaleGroupInstance.vm.instanceId, scaleGroupInstance.cloudName, "Created by LASIC for scale group on " + dateString, true)

            //wait for image to be available
            var imageState = ImageState.Unknown
            while (imageState != ImageState.Available) {
              Thread.sleep(10000)  // 10 seconds
              imageState = scaleGroup.getImageState(imageID)
            }


            //create the config
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
}