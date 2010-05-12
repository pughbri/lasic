package com.lasic

import cloud.LaunchConfiguration
import java.io.File

/**
 * Created by IntelliJ IDEA.
 * User: pughbc
 * Date: May 10, 2010
 * Time: 12:37:20 PM
 * To change this template use File | Settings | File Templates.
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