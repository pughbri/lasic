package com.lasic.cloud.noop

import com.lasic.util.Logging
import com.lasic.cloud.VM
import com.lasic.cloud._

class NoopVolume extends Volume with Logging{

  def id = "?"
  def info:VolumeInfo = {
    new VolumeInfo("?", 0, null, "null-zone", VolumeState.Unknown)
  }

  def delete = {
    throw new IllegalStateException()
  }

  def attachTo(vm:VM, deviceName:String) {
    throw new IllegalStateException()
  }

  def attachInfo:VolumeAttachmentInfo = {
    throw new IllegalStateException()
  }

  
}