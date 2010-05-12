package com.lasic.cloud.amazon

import com.lasic.{Cloud, VM}
import com.lasic.cloud.LaunchConfiguration

/**
 * Created by IntelliJ IDEA.
 * User: pughbc
 * Date: May 11, 2010
 * Time: 4:25:27 PM
 * To change this template use File | Settings | File Templates.
 */

class AmazonVM(val cloudInst: Cloud,val lc : LaunchConfiguration ) extends VM {
  val cloud: Cloud = cloudInst
  val launchConfiguration: LaunchConfiguration = lc
}