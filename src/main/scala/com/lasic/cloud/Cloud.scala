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
  def createVMs(launchConfig: LaunchConfiguration, numVMs : Int, startVM: Boolean):Array[VM]
  def start(vms: Array[VM])
  def reboot(vms: Array[VM])
  def shutdown(vms: Array[VM])
  def copyTo(vms: Array[VM], sourceFile: File, destinationAbsPath: String)
  def execute(vms: Array[VM], executableAbsPath: String)
}