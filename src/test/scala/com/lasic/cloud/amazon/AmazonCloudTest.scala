package com.lasic.cloud.amazon

import com.lasic.VM
import junit.framework.TestCase
import com.lasic.cloud.{LaunchConfiguration, AmazonCloud}

/**
 * Created by IntelliJ IDEA.
 * User: pughbc
 * Date: May 12, 2010
 * Time: 1:13:40 PM
 * To change this template use File | Settings | File Templates.
 */

class AmazonCloudTest extends TestCase("AmazonCloudTest") {
  def testStart() = { //disable test as it requires real keys and creates real instances
    if (false) {
      val cloud = new AmazonCloud()
      val lc: LaunchConfiguration = new LaunchConfiguration()
      lc.machineImage = "ami-714ba518" //base ubuntu image
      val vm: VM = new AmazonVM(cloud, lc)
      val vms = Array(vm)
      cloud.start(vms)
      Thread.sleep(30000); //give it a minute to come up
      cloud.terminate(vms)
    }
  }
}