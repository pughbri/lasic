package com.lasic.cloud.amazon

import com.lasic.cloud.VolumeState._
import com.lasic.cloud.VolumeInfo
import com.lasic.cloud._
import com.xerox.amazonws.ec2.{ Jec2, AttachmentInfo => XAttachmentInfo}
import java.util.{List => JList}
import collection.JavaConversions


//import amazon.{AmazonVolume, AmazonVM}
import java.lang.String
import java.util.Iterator
import java.util.{List => JList}
import com.xerox.amazonws.ec2.{AutoScaling, Jec2, ReservationDescription, AttachmentInfo => XAttachmentInfo}
import com.xerox.amazonws.ec2.InstanceType
import scala.collection.JavaConversions.asBuffer
import collection.JavaConversions
import com.lasic.cloud.MachineState._
import com.lasic.util.Logging
import com.lasic.{LasicProperties, VM, Cloud}

class AmazonVolume(ec2: Jec2, val id:String) extends Volume with Logging{

  def info:VolumeInfo = {
    val args = Array[String](id)
    val vi: com.xerox.amazonws.ec2.VolumeInfo = ec2.describeVolumes(args)(0)
    new VolumeInfo(vi.getVolumeId, vi.getSize.toInt, vi.getSnapshotId, vi.getZone, VolumeState.string2State(vi.getStatus))
  }



}