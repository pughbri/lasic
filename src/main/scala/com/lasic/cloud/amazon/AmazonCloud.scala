package com.lasic.cloud

import amazon.AmazonVM
import com.lasic.{VM, Cloud}

import java.io.File
import com.xerox.amazonws.ec2.Jec2

/**
 * Created by IntelliJ IDEA.
 * User: pughbc
 * Date: May 10, 2010
 * Time: 12:45:11 PM
 * To change this template use File | Settings | File Templates.
 */

class AmazonCloud extends Cloud {

  //todo:  get the key and secret from properties file
  val ec2: Jec2 = new Jec2("dummy", "value");

  override def createVMs(launchConfig: LaunchConfiguration, numVMs: Int, startVM: Boolean): Array[VM] = {
    createVMs(numVMs, startVM, () => new AmazonVM(this, launchConfig))
  }

  def start(vms: Array[VM]) {
    vms.foreach(vm => {
      //todo: get the launch configuration
      ec2.runInstances(null)
    }
      )

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