package com.lasic.cloud.mock


//import mock.MockVM
import com.lasic.cloud.{VM, Cloud}
import java.lang.String
import java.util.{Random}
import com.lasic.util.Logging
import com.lasic.cloud._
import java.util.{List => JList}

/**
 * User: Brian Pugh
 * Date: May 10, 2010
 */

class MockCloud(startupDelay: Int) extends Cloud with Logging {
  def this() = this (2);

  private val random = new Random(System.currentTimeMillis)

  override def createVMs(launchConfig: LaunchConfiguration, numVMs: Int, startVM: Boolean): List[VM] = {
    createVMs(numVMs, startVM) {new MockVM(startupDelay, this)}
  }

  def findVM(instanceId: String) = {
    require(instanceId != null, "must provide an instance id to find a vm")
    val vm = new MockVM(startupDelay, null, this)
    vm.instanceId = instanceId
    vm.isInit = true
    vm
  }

  def getStartupDelay(): Int = {
    startupDelay
  }

  def createVolume(config:VolumeConfiguration): Volume = {
    val id = "mock_volume-"+random.nextInt.toString
    new MockVolume(id, config )
  }

  def findVolume(id: String): Volume = {
    val config = new VolumeConfiguration(100, "snap", "zone")
    new MockVolume(id, config)
  }

  def allocateAddress() = {
    val random: Random = new Random()
    "10.255." + +random.nextInt(200) + "." + random.nextInt(200);
  }

  def releaseAddress(ip: String) = {
    logger.info("release ip [" + ip + "]")
  }

  def getScalingGroupClient() = {
    MockScalingGroupClient
  }


  def getLoadBalancerClient() = {
     MockLoadBalancerClient
  }
}