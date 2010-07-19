package com.lasic.cloud.mock

import junit.framework.TestCase
import com.lasic.{Cloud, VM}
import com.lasic.cloud.{VolumeConfiguration, LaunchConfiguration, MachineState}

/**
 *
 * User: Brian Pugh
 * Date: May 25, 2010
 */

class MockVolumeTest extends TestCase("MockVolumeTest") {
  def testDefaultedZoneCreate() = {
    val cloud:Cloud = new MockCloud(2)
    val config = new VolumeConfiguration(100, null, null)
    val volume = cloud.createVolume(config)
    assert( volume!=null )
    assert( volume.info.availabilityZone!=null )

  }


}