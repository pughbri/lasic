package com.lasic

import java.io.File

/**
 * Created by IntelliJ IDEA.
 * User: pughbc
 * Date: May 10, 2010
 * Time: 12:38:35 PM
 * To change this template use File | Settings | File Templates.
 */

trait VM {
  def start()
  def reboot()
  def stop()
  def copyTo(sourceFile: File, destinationAbsPath: String)
  def execute(executableAbsPath: String)
}