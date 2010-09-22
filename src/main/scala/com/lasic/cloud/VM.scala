package com.lasic.cloud

import ssh.{AuthFailureException, ConnectException, SshSession}
import java.io.File
import java.lang.String
import com.lasic.cloud.MachineState._
import com.lasic.util.Logging
import com.lasic.LasicProperties

/**
 * User: Brian Pugh
 * Date: May 10, 2010
 */

trait VM extends Logging {
//  protected val cloud: Cloud
//  protected var sshUp: Boolean = false
//
//  val launchConfiguration: LaunchConfiguration
//  var instanceId: String = null

  val launchConfiguration: LaunchConfiguration
  var instanceId:String = "?"


  /**Lasic configuration directory.  Key files should be in this directory.  Defaults to user.home/.lasic but
  can be overridden by setting the "lasic.config.dir" system property
   **/
  var baseLasicDir: String = {
    var prop = LasicProperties.getProperty("lasic.config.dir")
    if (prop == null) {
      prop = System.getProperty("user.home") + "/.lasic"
    }
    prop
  }


  /*==========================================================================================================
   Operations implemented in specific subclasses
  ==========================================================================================================*/

  /*==========================================================================================================
   Cloud operation methods
  ==========================================================================================================*/
  def startup()
//    {
//    cloud.start(List(this))
//  }

  def reboot()
//    {
//    cloud.reboot(List(this))
//  }

  def shutdown()
//    {
//    cloud.terminate(List(this))
//  }

  def copyTo(sourceFile: File, destinationAbsPath: String)

  def execute(executableAbsPath: String)

  def executeScript(scriptAbsPath: String, variables: Map[String, List[String]])

//  def attach(volumeInfo: VolumeInfo, devicePath: String): AttachmentInfo = {
//    cloud.attach(volumeInfo, this, devicePath)
//  }
//
//  def detach(volumeInfo: VolumeInfo, devicePath: String, force: Boolean): AttachmentInfo = {
//    cloud.detach(volumeInfo, this, devicePath, force)
//  }

  def associateAddressWith(ip: String)
//  {
//    try{
//      cloud.associateAddress(this, ip)
//      logger.info("Assigned elastic ip : " + ip + " to instance id: " + this.instanceId)
//    }
//    catch {
//      //todo: I think there are valid use cases for allowing an elastic ip assignment to fail, but not considering it fatal.
//      //todo: However, I think we should keep the state of the failure somehow then report it after completing the entire
//      //todo: run so the user doesn't miss it in the output
//      case e:Exception => logger.error("Unable to assign elastic ip : " + ip + " to instance id: " + this.instanceId)
//    }
//  }

  def disassociateAddress(ip: String)
//    {
//    cloud.disassociateAddress(ip)
//    logger.info("Unssigned elastic ip : " + ip + " from instance id: " + this.instanceId)
//  }





  /*==========================================================================================================
   State of vm methods
  ==========================================================================================================*/
  def getMachineState(): MachineState
//  = {
//    cloud.getState(this)
//  }

  /**
   * indicates that the server is up and the ssh daemon is running
   */
  def isInitialized(): Boolean
//  = {
//    if (sshUp) {
//      sshUp
//    }
//    else {
//
//      var session: SshSession = null
//      try {
//        session = createSshSession
//        connect(session, 0)
//        true
//      }
//      catch {
//        case e: AuthFailureException => {
//          //there seems to be a very short period of time when you get an auth failure
//          //because things aren't entirely initialized.  Give it one more shot to validate that this
//          //really is an authentication issue, not just a connection issue
//          logger.warn("isInitialized got an authentication failure.  Giving it one more shot with 10 second timeout to ensure this really is an authentication issue")
//          connect(session, 10)
//          true
//        }
//        case e: IllegalArgumentException => false //VM isn't running yet
//        case e: ConnectException => {logger.trace("VM in valid state, but not initialized: ", e); false}
//        case t: Throwable => throw t
//      }
//      finally {
//        if (session != null) {
//          session.disconnect
//        }
//
//      }
//    }
//
//  }

  def getPublicDns(): String

  def getPublicIpAddress(): String
//  = {
//    cloud.getPublicDns(this)
//  }

  def getPrivateDns(): String
//  = {
//    cloud.getPrivateDns(this)
//  }

//  def createSshSession: SshSession
//  = {
//    require(getMachineState == MachineState.Running, "VM is in state " + getMachineState + ".  Cannot open ssh connection unless it is Running")
//    val publicDns: String = getPublicDns()
//    require(publicDns != null, "VM in unexpected state " + getMachineState + " with no public DNS name. Cannot open ssh connection")
//
//    new SshSession(publicDns, launchConfiguration.userName, new File(baseLasicDir, launchConfiguration.key + ".pem"))
//  }

//  def withSshSession(timeout: Int)(callback: SshSession => Unit): Unit
//  = {
//    val session: SshSession = createSshSession
//    try {
//      connect(session, timeout)
//      callback(session)
//    }
//    finally {
//      session.disconnect
//    }
//  }

//  def connect(session: SshSession, timeout: Int): Unit
//  = {
//    require(getMachineState == MachineState.Running, "VM is in state " + getMachineState + ".  Cannot open ssh connection unless it is Running")
//
//    var connected = false
//    val startTime = System.currentTimeMillis
//
//    while (!connected) {
//      try {
//        session.connect()
//        connected = true
//        sshUp = true
//      }
//      catch {
//        //many times the VM hasn't quite started the ssh daemon even though it is up.  Give it a few tries with a
//        //delay between each try
//        case e: ConnectException => retryWithDelay(e)
//        case e: AuthFailureException => retryWithDelay(e)
//      }
//    }

//    def retryWithDelay(e: Exception): Unit
//    = {
//      {
//        if (System.currentTimeMillis - startTime > (timeout * 1000)) {
//          throw e
//        }
//        Thread.sleep(1000)
//      }
//    }
//  }


//  override def toString = if (instanceId != null) instanceId else "unknown vm vmId"
}