package com.lasic.cloud

import VolumeState._
import com.lasic.VM

trait Volume {
  def id:String
  def info:VolumeInfo
  def attachInfo:VolumeAttachmentInfo
  def attachTo(vm:VM, deviceName:String)
  def delete
}