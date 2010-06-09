package com.lasic

import cloud.ssh.{ConnectException, SshSession}
import cloud.{AttachmentInfo, VolumeInfo, MachineState, LaunchConfiguration}
import java.io.File
import java.lang.String
import com.lasic.cloud.MachineState._

/**
 * User: Brian Pugh
 * Date: May 10, 2010
 */

trait VM {
  protected val cloud: Cloud
  protected var sshUp: Boolean = false

  val launchConfiguration: LaunchConfiguration
  var instanceId: String = null


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
   Cloud operation methods
  ==========================================================================================================*/
  def startup() {
    cloud.start(List(this))
  }

  def reboot() {
    cloud.reboot(List(this))
  }

  def shutdown() {
    cloud.terminate(List(this))
  }

  def copyTo(sourceFile: File, destinationAbsPath: String)

  def execute(executableAbsPath: String)

  def attach(volumeInfo: VolumeInfo, devicePath: String): AttachmentInfo = {
    cloud.attach(volumeInfo, this, devicePath)
  }

  def detach(volumeInfo: VolumeInfo, devicePath: String, force: Boolean): AttachmentInfo = {
    cloud.detach(volumeInfo, this, devicePath, force)
  }

  def associateAddressWith(ip: String) {
    cloud.associateAddress(this, ip)
  }

  def disassociateAddress(ip: String) {
    cloud.disassociateAddress(ip)
  }





  /*==========================================================================================================
   State of vm methods
  ==========================================================================================================*/
  def getMachineState(): MachineState = {
    cloud.getState(this)
  }

  /**
   * indicates that the server is up and the ssh daemon is running
   */
  def isInitialized(): Boolean = {
    if (sshUp) {
      sshUp
    }
    else {
      val session: SshSession = createSshSession
      try {
        connect(session, 0)
        true
      }
      catch {
        case e: IllegalStateException => false
        case e: ConnectException => false
        case t: Throwable => throw t
      }
      finally {
        session.disconnect
      }
    }

  }

  def getPublicDns(): String = {
    cloud.getPublicDns(this)
  }

  def getPrivateDns(): String = {
    cloud.getPrivateDns(this)
  }

  protected def createSshSession: SshSession = {
    new SshSession()
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

    if (!(getMachineState == MachineState.Running)) {
      throw new IllegalStateException("VM is in state " + getMachineState + ".  Cannot open ssh connection unless it is Running")
    }
    val publicDns = getPublicDns
    if (publicDns == null) {
      throw new IllegalStateException("VM in unexpected state " + getMachineState + " with no public DNS name. Cannot open ssh connection")
    }

    var connected = false
    val startTime = System.currentTimeMillis
    while (!connected) {
      try {
        session.connect(getPublicDns, launchConfiguration.userName, new File(baseLasicDir, launchConfiguration.key + ".pem"))
        connected = true
        sshUp = true
      }
      catch {
        //many times the VM hasn't quite started the ssh daemon even though it is up.  Give it a few tries with a
        //delay between each try
        case e: ConnectException => {
          if (System.currentTimeMillis - startTime > (timeout * 1000)) {
            throw e
          }
          Thread.sleep(1000)
        }
      }
    }
  }


  override def toString = if (instanceId != null) instanceId else "unknown vm id"
}