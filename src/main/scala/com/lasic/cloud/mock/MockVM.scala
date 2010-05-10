package com.lasic.cloud.mock

import java.io.File
import com.lasic.{Cloud, VM}

/**
 * Created by IntelliJ IDEA.
 * User: pughbc
 * Date: May 10, 2010
 * Time: 1:17:30 PM
 * To change this template use File | Settings | File Templates.
 */

class MockVM(startupDelay: Int, cloud: Cloud) extends VM {

  def this(cloud: Cloud) = this (10, cloud)

  override def start() {
     Thread.sleep(startupDelay * 1000)
  }
  
  override def reboot() {

  }
  override def stop() {

  }
  override def copyTo(sourceFile: File, destinationAbsPath: String) {

  }
  override def execute(executableAbsPath: String) {
    
  }
}