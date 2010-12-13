package com.lasic.cloud.amazon

import com.lasic.cloud.VolumeInfo
import com.lasic.cloud._
import com.amazonaws.services.ec2.AmazonEC2Client
import com.amazonaws.services.ec2.model.{AttachVolumeRequest, DeleteVolumeRequest, DescribeVolumesRequest}
import java.lang.String
import scala.collection.JavaConversions.asBuffer
import com.lasic.util.Logging
import com.lasic.cloud.VM

class AmazonVolume(val awsClient: AmazonEC2Client, val id:String) extends Volume with Logging{

  def info:VolumeInfo = {
    val descVolumsReq = new DescribeVolumesRequest().withVolumeIds(id)
    val descVolumesResult = awsClient.describeVolumes(descVolumsReq)
    require(descVolumesResult.getVolumes.size == 1)
    val volume = descVolumesResult.getVolumes.get(0)
    new VolumeInfo(volume.getVolumeId, volume.getSize.intValue, volume.getSnapshotId, volume.getAvailabilityZone, VolumeState.string2State(volume.getState))
  }

  def attachInfo:VolumeAttachmentInfo = {
    val descVolumsReq = new DescribeVolumesRequest().withVolumeIds(id)
    val volume = awsClient.describeVolumes(descVolumsReq).getVolumes.get(0)
    val attachments = volume.getAttachments
    if ( attachments.size==0 )
          return null;

    val device = attachments.first.getDevice
    val instanceID = attachments.first.getInstanceId
    new VolumeAttachmentInfo(instanceID, device)
  }

  def delete = {
    val delRequest = new DeleteVolumeRequest().withVolumeId(id)
    awsClient.deleteVolume(delRequest)
  }

  def attachTo(vm:VM, deviceName:String) {
    val theVM = vm.asInstanceOf[AmazonVM]
    val attachVolumenReq = new AttachVolumeRequest().withVolumeId(id).withInstanceId(theVM.instanceId).withDevice(deviceName)
    awsClient.attachVolume(attachVolumenReq)
  }
}