package com.lasic.cloud.amazon

import com.lasic.{Cloud, VM}
import com.lasic.cloud.LaunchConfiguration

/**
 * User: Brian Pugh
 * Date: May 11, 2010
 */

class AmazonVM(val cloudInst: Cloud,val lc : LaunchConfiguration ) extends VM {
  val cloud: Cloud = cloudInst
  val launchConfiguration: LaunchConfiguration = lc
}