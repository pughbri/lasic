package com.lasic.cloud.ssh

import com.jcraft.jsch.UserInfo

/**
 * 
 * User: Brian Pugh
 * Date: May 14, 2010
 */

class MySshUserInfo extends UserInfo {
  def getPassphrase: String = {
     ""
  }


  def getPassword: String = {
    ""
  }


  def promptPassword(message: String): Boolean = {
    true
  }


  def promptPassphrase(message: String): Boolean = {
    true
  }


  def promptYesNo(message: String): Boolean = {
    true
  }


  def showMessage(message: String): Unit = {
  }
}