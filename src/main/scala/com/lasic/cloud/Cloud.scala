package com.lasic

import cloud.LaunchConfiguration

/**
 * Created by IntelliJ IDEA.
 * User: pughbc
 * Date: May 10, 2010
 * Time: 12:37:20 PM
 * To change this template use File | Settings | File Templates.
 */

trait Cloud {
  def startVM(launchConfig: LaunchConfiguration):VM
}