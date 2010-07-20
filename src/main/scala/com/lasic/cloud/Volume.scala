package com.lasic.cloud

import VolumeState._

trait Volume {
  def id:String
  def info:VolumeInfo
  def delete
}