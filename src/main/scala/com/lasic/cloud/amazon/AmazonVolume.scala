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

  def attachInfo:VolumeAttachmentInfo = {
    val args = Array[String](id)
    val vi: com.xerox.amazonws.ec2.VolumeInfo = ec2.describeVolumes(args)(0)
    val infoList = vi.getAttachmentInfo
    if ( infoList.size==0 )
      return null;

//    val args = List(vmId)
//    val volList:JList = ec2.describeVolumes(args)
//    val vi: com.xerox.amazonws.ec2.VolumeInfo = ec2.describeVolumes(args).get(0)
//    val list:List = vi.getAttachmentInfo
//    if ( list.size==0 )
//      return null
    val device = infoList.first.getDevice
    val instanceID = infoList.first.getInstanceId
    new VolumeAttachmentInfo(instanceID, device)


  }

  def delete = {
    ec2.deleteVolume(id)
  }

  def attachTo(vm:VM, deviceName:String) {
    // Ensure we only attach to MockVM!
    val theVM = vm.asInstanceOf[AmazonVM]
    ec2.attachVolume( id, theVM.instanceId, deviceName)
  }
}