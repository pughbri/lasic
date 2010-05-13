package com.lasic

import cloud.LaunchConfiguration
import java.io.File

/**
 * User: Brian Pugh
 * Date: May 10, 2010
 */
trait Cloud {
  
  def createVMs(launchConfig: LaunchConfiguration, numVMs: Int, startVM: Boolean): Array[VM]

  def start(vms: Array[VM])

  def reboot(vms: Array[VM])

  def terminate(vms: Array[VM])

  def copyTo(vms: Array[VM], sourceFile: File, destinationAbsPath: String)

  def execute(vms: Array[VM], executableAbsPath: String)

  protected def createVMs(numVMs: Int, startVM: Boolean, callback: () => VM): Array[VM] = {
    var vms = new Array[VM](numVMs);
    for (i <- 0 until numVMs) {
      val vm: VM = callback()
      vms(i) = vm
    }

    if (startVM) {
      start(vms)
    }

    return vms
  }
}