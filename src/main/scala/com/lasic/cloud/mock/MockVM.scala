package com.lasic.cloud.mock

import java.io.File
import com.lasic.cloud.VM
import java.lang.String
import com.lasic.cloud.MachineState._
import util.Random
import com.lasic.cloud.ssh.{SshSession, BashPreparedScriptExecution}
import com.lasic.cloud.{MachineState, LaunchConfiguration}
import com.lasic.concurrent.ops._
import com.lasic.values.{ResolvedScriptDefinition, ScriptDefinition}

/**
 * User: Brian Pugh
 * Date: May 10, 2010
 */

object MockVM {
  val random = new Random(System.currentTimeMillis)
}

class MockVM(delay: Int, val launchConfiguration: LaunchConfiguration, cloud: MockCloud) extends VM {
  //start()

  var isInit = false
  var privateDNS = "?"
  var publicDNS = "?"

  def this(cloud: MockCloud) = this (2, null, cloud)

  def this(delay: Int, cloud: MockCloud) = this (delay, null, cloud)

  //  case class StateChange(state: MachineState.Value, delay: Int)

  //  val cloud: Cloud = cloud

  var machineState: MachineState = Unknown


  override def startup() {
    spawn("startup mock vm") {
      machineState.synchronized {
        //Thread.sleep(delay * 1000)
        machineState = MachineState.Pending
        Thread.sleep(delay * 1000)
        instanceId = "mockvm-" + MockVM.random.nextInt.toString
        publicDNS = "1.2.3." + MockVM.random.nextInt(256).toString
        privateDNS = "4.5.6." + MockVM.random.nextInt(256).toString
        machineState = MachineState.Running
        Thread.sleep(delay * 1000)
        isInit = true
      }
    }
  }

  override def reboot() {
    spawn("roboot mock VM") {
      machineState.synchronized {
        machineState = MachineState.Rebooting
        Thread.sleep(delay * 1000)
        machineState = MachineState.Running
      }
    }
  }

  override def shutdown() {
    spawn("shutdown mock vm") {
      machineState.synchronized {
        machineState = MachineState.ShuttingDown
        Thread.sleep(delay * 1000)
        machineState = MachineState.Terminated
      }
    }
  }

  override def copyTo(sourceFile: File, destinationAbsPath: String) {
    //withDelay(logger.info("copying file " + sourceFile.getAbsoluteFile + " to " + destinationAbsPath))
  }

  override def execute(executableAbsPath: String) {
    //withDelay(logger.info("executing " + executableAbsPath))
  }

  override def executeScript(scriptDefinition: ResolvedScriptDefinition) {
    class MockSshSession extends SshSession(getPublicDns, "ubuntu", new File("")) {
      override def sendCommand(cmd: String) = {
        logger.info("sending command [" + cmd + "] to " + userName + "@" + dnsName)
        0
      }
    }
    withDelay {
      logger.debug("prepare to execute " + scriptDefinition.scriptName + ".  Env vars: " + scriptDefinition.scriptArguments.mkString(", "))
      new BashPreparedScriptExecution(new MockSshSession, scriptDefinition.scriptName, scriptDefinition.scriptArguments).execute()

    }
  }

  def associateAddressWith(ip: String) {
    if (ip == null || ip == "") {
      logger.error("Assigned elastic ip : " + ip + " to instance id: " + this.instanceId)
    }
    else {
      logger.info("Assigned elastic ip : " + ip + " to instance id: " + this.instanceId)
      spawn("associate ip with an instance") {
        Thread.sleep(delay * 1000)
        publicDNS = ip
      }
    }
  }

  def disassociateAddress(ip: String) {

  }

  def getMachineState(): MachineState = {
    machineState
  }

  def getPublicDns(): String = publicDNS

  def getPublicIpAddress(): String = publicDNS

  def getPrivateDns(): String = privateDNS

  def connect(session: SshSession, timeout: Int) {

  }

  def retryWithDelay(e: Exception) {

  }

  def createSshSession: SshSession = {
    null
  }

  private def withDelay(callback: => Unit): Unit = {
    Thread.sleep(delay * 1000)
    callback
  }

  override def isInitialized(): Boolean = {
    Thread.sleep(delay * 1000)
    isInit
  }

  def assignState(s: MachineState.Value) {
    if (instanceId == null)
      instanceId = "i-" + new Random().nextInt(5000)
    machineState = s
  }

}