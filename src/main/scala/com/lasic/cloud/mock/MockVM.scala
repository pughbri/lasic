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

class MockVM(delay: Int, cloudInst: Cloud) extends VM {
  def this(cloud: Cloud) = this (2, cloud)

  val cloud: Cloud = cloudInst
  val delayInMillis = delay * 1000

  override def start() {
    //todo: replace all these sleeps with a single closure that sleeps then executes what we pass into the closure
    Thread.sleep(delayInMillis)
    super.start()

  }

  override def reboot() {
    Thread.sleep(delayInMillis)
    super.reboot()

  }

  override def shutdown() {
    Thread.sleep(delayInMillis)
    super.shutdown()
  }

  override def copyTo(sourceFile: File, destinationAbsPath: String) {
    Thread.sleep(delayInMillis)
    super.copyTo(sourceFile, destinationAbsPath)
  }

  override def execute(executableAbsPath: String) {
    Thread.sleep(delayInMillis)
    super.execute(executableAbsPath)
  }

  
}