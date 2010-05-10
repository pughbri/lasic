package com.lasic.cloud

import com.lasic.{VM, Cloud}

import mock.MockVM

/**
 * Created by IntelliJ IDEA.
 * User: pughbc
 * Date: May 10, 2010
 * Time: 12:45:11 PM
 * To change this template use File | Settings | File Templates.
 */

class AmazonCloud  extends Cloud {

  override def startVM(launchConfig: LaunchConfiguration):VM = {
    new MockVM(null)
  }

  
}