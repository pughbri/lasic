package com.lasic.cloud.amazon

import java.util.{List => JList}
import scala.collection.JavaConversions.asBuffer


import com.lasic.cloud.VM
import java.io.File
import collection.immutable.Map
import java.lang.String
import java.util.{List => JList}
import com.lasic.cloud.MachineState._
import com.lasic.cloud.{MachineState, LaunchConfiguration}
import com.lasic.cloud.ssh.{ConnectException, AuthFailureException, SshSession, BashPreparedScriptExecution}
import com.amazonaws.services.ec2.AmazonEC2Client
import com.amazonaws.services.ec2.model._

/**
 * User: Brian Pugh
 * Date: May 11, 2010
 */

class AmazonVM(awsClient: AmazonEC2Client, val launchConfiguration: LaunchConfiguration, val timeout: Int) extends VM {
  def this(awsClient: AmazonEC2Client, launchConfiguration: LaunchConfiguration) = this (awsClient, launchConfiguration, 10)

  private var sshUp = false;

  def startup() {
    val request = MappingUtil.createAWSRunInstancesRequest(launchConfiguration)
    val rir = awsClient.runInstances(request)
    require(rir.getReservation.getInstances.size == 1, "Excepted behavior.  AWS api return multiple instances when we request that only one be created.")
    rir.getReservation.getInstances.foreach(instance => instanceId = instance.getInstanceId)
  }

  def reboot() {

  }

  def shutdown() {
    logger.debug("terminating " + instanceId)
    var instanceList = new java.util.ArrayList[String]
    instanceList.add(instanceId)
    var instances = new TerminateInstancesRequest().withInstanceIds(instanceList)
    awsClient.terminateInstances(instances)
  }

  def associateAddressWith(ip: String) {
    try {
      var aar = new AssociateAddressRequest().withInstanceId(instanceId).withPublicIp(ip)
      awsClient.associateAddress(aar)
      logger.info("Assigned elastic ip : " + ip + " to instance id: " + this.instanceId)
    }
    catch {
      //todo: I think there are valid use cases for allowing an elastic ip assignment to fail, but not considering it fatal.
      //todo: However, I think we should keep the state of the failure somehow then report it after completing the entire
      //todo: run so the user doesn't miss it in the output
      case e: Exception => logger.error("Unable to assign elastic ip : " + ip + " to instance id: " + this.instanceId)
    }
  }

  def disassociateAddress(ip: String) {
    var dar = new DisassociateAddressRequest().withPublicIp(ip)
    awsClient.disassociateAddress(dar)
    logger.info("Unssigned elastic ip : " + ip + " from instance id: " + this.instanceId)
  }


  def getMachineState(): MachineState = {
    MachineState.withName(getInstance(this).getState.getName)
  }

  private def getInstance(vm: VM): Instance = {
    var instanceList = new java.util.ArrayList[String]
    instanceList.add(instanceId)
    
    var dir = new DescribeInstancesRequest().withInstanceIds(instanceList)
    val dirr = awsClient.describeInstances(dir)
    if (dirr.getReservations.size != 1) {
      throw new IllegalStateException("expected a single reservation description for instance vmId " + vm.instanceId + " but got " + dirr.getReservations.get(0).getInstances.size)
    }

    val instances = dirr.getReservations.get(0).getInstances
    if (dirr.getReservations.get(0).getInstances.size != 1) {
      throw new IllegalStateException("expected a single instance for instance vmId " + vm.instanceId + " but got " + instances.size)
    }

    instances.get(0)
  }


  def isInitialized(): Boolean = {
    if (sshUp) {
      sshUp
    }
    else {

      var session: SshSession = null
      try {
        session = createSshSession
        connect(session, 0)
        true
      }
      catch {
        case e: AuthFailureException => {
          //there seems to be a very short period of time when you get an auth failure
          //because things aren't entirely initialized.  Give it one more shot to validate that this
          //really is an authentication issue, not just a connection issue
          logger.warn("isInitialized got an authentication failure.  Giving it one more shot with 10 second timeout to ensure this really is an authentication issue")
          connect(session, 10)
          true
        }
        case e: IllegalArgumentException => false //VM isn't running yet
        case e: ConnectException => {logger.trace("VM in valid state, but not initialized: ", e); false}
        case t: Throwable => throw t
      }
      finally {
        if (session != null) {
          session.disconnect
        }

      }
    }

  }

  def getPublicDns(): String = {
    getInstance(this).getPublicDnsName
  }

  def getPublicIpAddress(): String = {
    getInstance(this).getPublicIpAddress
  }

  def getPrivateDns(): String = {
    getInstance(this).getPrivateDnsName()

  }

  def createSshSession: SshSession = {
    require(getMachineState == MachineState.Running, "VM is in state " + getMachineState + ".  Cannot open ssh connection unless it is Running")
    val publicDns: String = getPublicDns()
    require(publicDns != null, "VM in unexpected state " + getMachineState + " with no public DNS name. Cannot open ssh connection")

    new SshSession(publicDns, launchConfiguration.userName, new File(baseLasicDir, launchConfiguration.key + ".pem"))
  }

  def withSshSession(timeout: Int)(callback: SshSession => Unit): Unit = {
    val session: SshSession = createSshSession
    try {
      connect(session, timeout)
      callback(session)
    }
    finally {
      session.disconnect
    }
  }

  def connect(session: SshSession, timeout: Int): Unit = {
    require(getMachineState == MachineState.Running, "VM is in state " + getMachineState + ".  Cannot open ssh connection unless it is Running")

    var connected = false
    val startTime = System.currentTimeMillis

    while (!connected) {
      try {
        session.connect()
        connected = true
        sshUp = true
      }
      catch {
        //many times the VM hasn't quite started the ssh daemon even though it is up.  Give it a few tries with a
        //delay between each try
        case e: ConnectException => retryWithDelay(e, startTime)
        case e: AuthFailureException => retryWithDelay(e, startTime)
      }
    }
  }

  def retryWithDelay(e: Exception, startTime:Long) = {
    {
      if (System.currentTimeMillis - startTime > (timeout * 1000)) {
        throw e
      }
      Thread.sleep(1000)
    }
  }


  def copyTo(sourceFile: File, destinationAbsPath: String) {
    withSshSession(timeout) {
      session => session.sendFile(sourceFile, destinationAbsPath)
    }
  }

  def execute(executableAbsPath: String) {
    withSshSession(timeout) {
      session => session.sendCommand(executableAbsPath)
    }
  }


  def executeScript(scriptAbsPath: String, variables: Map[String, List[String]]) = {
    withSshSession(timeout) {
      session => new BashPreparedScriptExecution(session, scriptAbsPath, variables).execute()
    }
  }
}