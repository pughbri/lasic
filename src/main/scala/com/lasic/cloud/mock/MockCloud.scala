package com.lasic.cloud


import mock.MockVM
import com.lasic.{VM, Cloud}
import java.io.File

/**
 * User: Brian Pugh
 * Date: May 10, 2010
 */

class MockCloud(startupDelay: Int) extends Cloud {
  def this() = this (2);

  override def createVMs(launchConfig: LaunchConfiguration, numVMs: Int, startVM: Boolean): Array[VM] = {
    createVMs(numVMs, startVM, () => new MockVM(startupDelay, this))
  }

  def start(vms: Array[VM]) {
    vms.foreach(vm => System.out.println("starting vm [" + vm + "]...."))
  }

  def getStartupDelay(): Int = {
    startupDelay
  }

  def reboot(vms: Array[VM]) {

    vms.foreach(vm => System.out.println("rebooting vm [" + vm + "]...."))
  }

  def terminate(vms: Array[VM]) {
    vms.foreach(vm => System.out.println("shutting down vm [" + vm + "]...."))
  }

  def copyTo(vms: Array[VM], sourceFile: File, destinationAbsPath: String) {
    vms.foreach(vm => System.out.println("copying file to vm [" + vm + "]...."))
  }

  def execute(vms: Array[VM], executableAbsPath: String) {
    vms.foreach(vm => System.out.println("executing on vm [" + vm + "]...."))
  }

}