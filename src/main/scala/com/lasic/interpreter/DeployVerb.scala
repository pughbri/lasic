package com.lasic.interpreter

import collection.immutable.List
import com.lasic.cloud.VolumeState._
import com.lasic.cloud.MachineState._
import com.lasic.model._
import com.lasic.exceptions.NotFoundException
import com.lasic.interpreter.VerbUtil._
import com.lasic.util.Logging
import com.lasic.concurrent.ops._
import com.lasic.cloud._
import com.lasic.LasicProperties
import java.util.Date
import com.lasic.util._

/**
 * Launches VM, attaches volumes, runs the "install" action for each one, and brings up scale groups.
 */
class DeployVerb(val cloud: Cloud, val program: LasicProgram) extends Verb with Logging with ActionRunnerUtil {
  protected val nodes: List[NodeInstance] = program.find("//node[*][*]").map(_.asInstanceOf[NodeInstance])
  protected val scaleGroups: List[ScaleGroupInstance] = program.find("//scale-group[*]").map(_.asInstanceOf[ScaleGroupInstance])
  protected val loadBalancers: List[LoadBalancerInstance] = program.find("//load-balancer[*]").map(_.asInstanceOf[LoadBalancerInstance])
  protected val vmState: Map[VMHolder, VMState] = {
    Map.empty ++ (nodes.map {node => (node, new VMState)} ::: scaleGroups.map {scaleGroup => (scaleGroup, new VMState)})
  }
  private var volumes: List[VolumeInstance] = nodes.map(_.volumes).flatten

  private val sleepDelay = LasicProperties.getProperty("SLEEP_DELAY", "10000").toInt

  private def validateProgram {}

  private def launchAllAMIs {
    nodes.foreach {
      node =>
        spawn("Launch node instances") {
          node.vm = cloud.createVM(LaunchConfiguration.build(node), true)
          logger.debug("created instance for node: " + node)
        }
    }
    scaleGroups.foreach {
      scaleGroup =>
        spawn("launch scale group instances") {
          scaleGroup.vm = cloud.createVM(LaunchConfiguration.buildMaster(scaleGroup.configuration), true)
        }
    }
  }

  private def resolveExistingVolumes {
    var ok = true
    volumes.foreach(volInst =>
      if (volInst.id != "") {
        val volume = cloud.findVolume(volInst.id)
        if (volume == null) {
          logger.error("Volume with instance id '" + volInst.id + "' does not exist.")
          ok = false
        } else {
          volInst.volume = volume
        }
      }
    )
    if (!ok) {
      throw new NotFoundException("Specified volumes not found!")
    }
  }

  private def createAllVolumes {
    volumes.foreach(volInst =>
      spawn("create/find volumes") {
        if (volInst.id == "") {
          volInst.volume = cloud.createVolume(VolumeConfiguration.build(volInst))
        }
      }
    )
  }


  private def waitForAMIsToBoot {
    waitForVMState({vmHolder => vmHolder.vm == null || !vmHolder.vm.isInitialized}, "Waiting for machines to boot: ")
    logger.info("Booted IDs are: " + nodes.map(t => showValue(t.vmId) + ":" + showValue(t.vmState)))
  }

  private def waitForScaleGroupsConfigured() {
    VerbUtil.waitForVMState(scaleGroups, {vmHolder => vmHolder.vm.getMachineState != MachineState.ShuttingDown && vmHolder.vm.getMachineState != MachineState.Terminated}, "Waiting for Scale Groups to come up: ")
  }

  private def waitForVMState(state: MachineState, statusString: String) {
    waitForVMState({vmHolder => vmHolder.vm == null || vmHolder.vm.getMachineState != state}, statusString)
  }

  private def waitForActionItems {
    waitForVMState({vmHolder => !(vmState(vmHolder).ipsComplete)}, "Waiting for action items to run: ")
  }

  private def waitForVMState(test: VMHolder => Boolean, statusString: String) {
    VerbUtil.waitForVMState(nodes ::: scaleGroups, test, statusString)
  }


  private def waitForVolumeState(state: VolumeState, statusString: String) {
    var waiting = volumes.filter(v => v.volume.info.state != state)
    while (waiting.size > 0) {
      val descriptions: List[String] = waiting.map(v => v.volume.id + ":" + v.volume.info.state)
      logger.info(statusString + descriptions)
      Thread.sleep(sleepDelay)
      waiting = volumes.filter(v => v.volume.info.state != state)
    }
  }

  private def waitForVolumes {
    waitForVolumeState(VolumeState.Available, "Waiting for volumes to be created: ")
  }

  private def attachAllVolumes {
    volumes.foreach(
      volInst => {
        volInst.volume.attachTo(volInst.parentNodeInstance.vm, volInst.device)
      }
      )
  }

  private def waitForVolumesToAttach {
    waitForVolumeState(VolumeState.InUse, "Waiting for volumes to attach: ")
  }


  private def printBoundLasicProgram {
    PrintLine("paths {")
    nodes foreach {
      node => PrintLine("    " + node.path + ": \"" + node.vmId + "\"  // public=" + showValue(node.vmPublicDns) + "\tprivate=" + showValue(node.vmPrivateDns))
    }
    scaleGroups foreach {
      scaleGroup =>
        PrintLine("    " + scaleGroup.path + ": \"" + scaleGroup.cloudName + "\"")
        PrintLine("    " + scaleGroup.configuration.path + ": \"" + scaleGroup.configuration.cloudName + "\"")
    }
    volumes foreach {
      volumeInst => PrintLine("    " + volumeInst.path + ": \"" + volumeInst.volume.id + "\"")
    }

    loadBalancers foreach {
      loadBalancer => PrintLine("    " + loadBalancer.path + ": \"" + loadBalancer.cloudName + "\" // dns=" + loadBalancer.dnsName)
    }
    PrintLine("}")
  }

  private def createLoadBalancers {
    loadBalancers foreach {
      lbInst =>
        spawn("create load balancers") {
          lbInst.dnsName = cloud.getLoadBalancerClient().createLoadBalancer(lbInst.cloudName,
            lbInst.lbPort,
            lbInst.instancePort,
            lbInst.protocol,
            lbInst.sslcertificate,
            List(LasicProperties.getProperty("availability_zone", "us-east-1d")))
        }
    }
  }

   private def waitForLoadBalancersToInitialize() {
    var waiting = loadBalancers filter(_.dnsName == "")
    while (waiting.size > 0) {
      val descriptions = waiting map(_.cloudName)
      logger.info("waiting for loadbalancers to initialize:" + descriptions)
      Thread.sleep(sleepDelay)
      waiting = loadBalancers filter(_.dnsName == "")
    }
  }

  def setLoadBalancerNames {
    loadBalancers foreach {
      lbInst =>
      //create unique names
        val dateString = new java.text.SimpleDateFormat("yyyy-MM-dd-HH-mm-ss").format(new Date())
        lbInst.cloudName = lbInst.localName + "-" + dateString
    }
  }

  def registerInstancesWithLoadBalancer {
    nodes foreach {
      node =>
       val loadBalancers = node.parent.loadBalancers map (ScriptResolver.resolveArgumentValue(node, _))
       loadBalancers foreach {
          loadBalancer =>
             cloud.getLoadBalancerClient.registerWith(loadBalancer, node.vm)
       }
    }
  }

  def doit() {

    // Error checks before doing anything
    validateProgram

    // Startup everything that needs it
    resolveExistingVolumes
    launchAllAMIs
    createAllVolumes
    setLoadBalancerNames
    setScaleGroupNames
    createLoadBalancers


    // Wait for all resources to be created before proceeding
    waitForAMIsToBoot
    waitForVolumes
    waitForLoadBalancersToInitialize

    // Attach all volumes in preparation for setup
    attachAllVolumes
    waitForVolumesToAttach

    // Configure all the nodes
    startAsyncRunAction("install")

    // Wait for all nodes to be configured
    waitForActionItems

    createScaleGroups(cloud.getScalingGroupClient)

    registerInstancesWithLoadBalancer

    // Wait for scale groups to be configured
    waitForScaleGroupsConfigured

    waitForElasticIpDnsChange("install")
    // Print out the bound program so the user can see the IDs we are manipulating
    printBoundLasicProgram
  }


  override def terminate {
    printBoundLasicProgram
  }
}