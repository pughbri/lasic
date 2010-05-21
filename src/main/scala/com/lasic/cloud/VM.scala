package com.lasic

import cloud.ssh.{ConnectException, SshSession}
import cloud.{AttachmentInfo, VolumeInfo, MachineState, LaunchConfiguration}
import java.io.File
import java.lang.String

/**
 * User: Brian Pugh
 * Date: May 10, 2010
 */

trait VM {
  val cloud: Cloud
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
  def start() {
    cloud.start(Array(this))
  }

  def reboot() {
    cloud.reboot(Array(this))
  }

  def shutdown() {
    cloud.terminate(Array(this))
  }

  def copyTo(sourceFile: File, destinationAbsPath: String)

  def execute(executableAbsPath: String)

  def attach(volumeInfo: VolumeInfo, devicePath: String): AttachmentInfo = {
    cloud.attach(volumeInfo, this, devicePath)
  }

  def detach(volumeInfo: VolumeInfo, devicePath: String, force: Boolean): AttachmentInfo = {
    cloud.detach(volumeInfo, this, devicePath, force)
  }

  def associateAddressWith(ip: String)  {
    cloud.associateAddress(this, ip)
  }

  def disassociateAddress(ip: String) {
     cloud.disassociateAddress(ip)
  }

  



  /*==========================================================================================================
   State of vm methods
  ==========================================================================================================*/
  def getState(): MachineState.Value = {
    cloud.getState(this)
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

    def connect(publicDns: String): Unit = {
      var connected = false
      var numAttempts = 0
      val startTime = System.currentTimeMillis
      while (!connected) {
        try {
          session.connect(publicDns, launchConfiguration.userName, new File(baseLasicDir, launchConfiguration.key + ".pem"))
          connected = true
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

    try {
      if (!(getState == MachineState.Running)) {
        throw new IllegalStateException("VM is in state " + getState + ".  Cannot operate on it unless it is Running")
      }
      val publicDns = getPublicDns
      if (publicDns == null) {
        throw new IllegalStateException("VM in unexpected state " + getState + " with no public DNS name.")
      }

      connect(publicDns)
      callback(session)
    }
    finally {
      session.disconnect
    }

  }

}