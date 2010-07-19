package com.lasic.cloud

import com.lasic.LasicProperties

/**
 * Created by IntelliJ IDEA.
 * User: lmonson
 * Date: Jul 19, 2010
 * Time: 4:25:55 PM
 * To change this template use File | Settings | File Templates.
 */

class VolumeConfiguration(val size: Int, val snapID: String, zone: String) {
  val availabilityZone:String = if( zone==null ) LasicProperties.getProperty("availability_zone", "us-east-1d") else zone
  
}