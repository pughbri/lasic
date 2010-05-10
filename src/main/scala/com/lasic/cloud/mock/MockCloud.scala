package com.lasic.cloud


import mock.MockVM
import com.lasic.{VM, Cloud}

/**
 * Created by IntelliJ IDEA.
 * User: pughbc
 * Date: May 10, 2010
 * Time: 12:46:30 PM
 * To change this template use File | Settings | File Templates.
 */

class MockCloud(startupDelay: Int) extends Cloud {

  def this() = this(5);
  
  override def startVM(launchConfig: LaunchConfiguration):VM = {
    val vm: MockVM = new MockVM(startupDelay, this)
    vm.start()
    return vm
  }

  def getStartupDelay(): Int = {
    startupDelay
  }
}