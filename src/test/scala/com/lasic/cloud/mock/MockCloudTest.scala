package com.lasic.cloud

import junit.framework._
import com.lasic.cloud.VM
import mock.MockCloud


/**
 * Created by IntelliJ IDEA.
 * User: pughbc
 * Date: May 10, 2010
 * Time: 1:04:21 PM
 * To change this template use File | Settings | File Templates.
 */

class MockCloudTest extends TestCase("MockCloudTest") {
  def testStartVM() = {
    val cloud = new MockCloud(1)
    val time1 = System.currentTimeMillis();
    val numInstances: Int = 3
    val vms: List[VM] = cloud.createVMs(new LaunchConfiguration, numInstances, true)
    val time2 = System.currentTimeMillis();
    assert(vms.size == numInstances);
    //todo: test that cloud.startup was called with the Array[VM]

  }

}