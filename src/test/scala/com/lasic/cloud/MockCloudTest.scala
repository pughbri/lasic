package com.lasic.cloud

import junit.framework._
import com.lasic.VM;


/**
 * Created by IntelliJ IDEA.
 * User: pughbc
 * Date: May 10, 2010
 * Time: 1:04:21 PM
 * To change this template use File | Settings | File Templates.
 */

class MockCloudTest extends TestCase("MockCloudTest") {
  def testStartVM() = {
    val cloud = new MockCloud(2)
    val time1 = System.currentTimeMillis();
    val vm: VM = cloud.startVM(new LaunchConfiguration())
    val time2 = System.currentTimeMillis();
    assert( ((time2 - time1) * 1000) >= (cloud.getStartupDelay()) )

  }
}