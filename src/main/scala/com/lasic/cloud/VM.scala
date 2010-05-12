package com.lasic

import cloud.LaunchConfiguration
import java.io.File

/**
 * Created by IntelliJ IDEA.
 * User: pughbc
 * Date: May 10, 2010
 * Time: 12:38:35 PM
 * To change this template use File | Settings | File Templates.
 */

trait VM {
  val cloud: Cloud
  val launchConfiguration : LaunchConfiguration
  var instanceId: String = null

  def start() {
    cloud.start(Array(this))
  }
  def reboot(){
    cloud.reboot(Array(this))
  }
  def shutdown(){
    cloud.terminate(Array(this))
  }
  def copyTo(sourceFile: File, destinationAbsPath: String){
    cloud.copyTo(Array(this), sourceFile, destinationAbsPath)
  }
  def execute(executableAbsPath: String){
    cloud.execute(Array(this), executableAbsPath)
  }

}