package com.lasic.cloud

import com.lasic.{VM, Cloud}

import java.io.File

/**
 * Created by IntelliJ IDEA.
 * User: pughbc
 * Date: May 10, 2010
 * Time: 12:45:11 PM
 * To change this template use File | Settings | File Templates.
 */

class AmazonCloud extends Cloud {
  override def createVMs(launchConfig: LaunchConfiguration, numVMs: Int, startVM: Boolean): Array[VM] = {
    return new Array[VM](0);
  }

  def start(vms: Array[VM]) {

  }

  def reboot(vms: Array[VM]) {

  }

  def shutdown(vms: Array[VM]) {

  }

  def copyTo(vms: Array[VM], sourceFile: File, destinationAbsPath: String) {

  }

  def execute(vms: Array[VM], executableAbsPath: String) {

  }


}