package com.lasic

import cloud.ssh.SshSession
import cloud.{MachineState, LaunchConfiguration}
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


  /**lasic configuration directory.  Key files should be in this directory fixed to home dire but oveerridable with prop**/
  var baseLasicDir: String = {

    var prop = LasicProperties.getProperty("lasic.config.dir")
    if (prop == null) {
      prop = System.getProperty("user.home") + "/.lasic"
    }
    prop
  }

  def start() {
    cloud.start(Array(this))
  }

  def reboot() {
    cloud.reboot(Array(this))
  }

  def shutdown() {
    cloud.terminate(Array(this))
  }

  def getState(): MachineState.Value = {
    cloud.getState(this)
  }

  def getPublicDns(): String = {
    cloud.getPublicDns(this)
  }

  def getPrivateDns(): String = {
    cloud.getPrivateDns(this)
  }

  def copyTo(sourceFile: File, destinationAbsPath: String)

  def execute(executableAbsPath: String)

  protected def createSshSession: SshSession = {
    new SshSession()
  }

  def withSshSession(callback: SshSession => Unit): Unit = {
    val session: SshSession = createSshSession
    try {
      if (!(getState == MachineState.Running)) {
        throw new IllegalStateException("VM is in state " + getState + ".  Cannot operate on it unless it is Running")
      }
      val publicDns = getPublicDns
      if (publicDns == null) {
        throw new IllegalStateException("VM in unexpected state " + getState + " with no public DNS name.")
      }

      //todo:  why should session just throw an exception if it can't connect?  Then I can communicate back why it failed.
      if (!session.connect(publicDns, launchConfiguration.userName, new File(baseLasicDir, launchConfiguration.key + ".pem"))) {
        throw new RuntimeException("unable to connect to vm with instance id [" + instanceId + "]")
      }
      callback(session)
    }
    finally {
      session.disconnect
    }

  }

}