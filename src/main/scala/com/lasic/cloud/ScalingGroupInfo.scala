package com.lasic.cloud

/**
 *
 * @author Brian Pugh
 */

class ScalingGroupInfo(val scaleGroupName: String,
                       val launchConfigurationName: String,
                       val minSize: Int,
                       val maxSize: Int,
                       val desiredCapacity: Int,
                       val cooldown: Int,
                       val availabilityZones: List[String],
                       val instances: List[String])