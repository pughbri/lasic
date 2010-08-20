package com.lasic.cloud

import java.util.Calendar
import VolumeState._

/**
 *
 * User: Brian Pugh
 * Date: May 20, 2010
 */

class VolumeInfo (val id: String, size: Int, snapID: String, availabilityZone: String, val state: VolumeState) extends VolumeConfiguration(size,snapID,availabilityZone) {
}

