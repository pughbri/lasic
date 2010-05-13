package com.lasic

import cloud.LaunchConfiguration
import java.io.File

/**
 * User: Brian Pugh
 * Date: May 10, 2010
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