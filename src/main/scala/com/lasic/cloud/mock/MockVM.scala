package com.lasic.cloud.mock

import java.io.File
import com.lasic.{Cloud, VM}
import com.lasic.cloud.LaunchConfiguration

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
  val launchConfiguration: LaunchConfiguration = null

  override def start() {
    withDelay(() => super.start())
  }

  override def reboot() {
    withDelay(() => super.reboot())
  }

  override def shutdown() {
    withDelay(() => super.shutdown())
  }

  override def copyTo(sourceFile: File, destinationAbsPath: String) {
    withDelay(() => super.copyTo(sourceFile, destinationAbsPath))
  }

  override def execute(executableAbsPath: String) {
    withDelay(() => super.execute(executableAbsPath))
  }

  def withDelay(callback: () => Unit): Unit = {
    Thread.sleep(delay* 1000)
    callback()
  }


}