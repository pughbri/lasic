package com.lasic.cloud.mock

import com.lasic.{VDisk, Cloud}
import VDisk.VDiskState._

class MockVDisk( override val cloud:Cloud, override val size:Int, override val snap:String, override val zone:String) extends VDisk {
  var diskState: VDiskState = Unknown

}

