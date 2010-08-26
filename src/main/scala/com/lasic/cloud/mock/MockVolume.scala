package com.lasic.cloud.mock

import com.lasic.util.Logging
import java.util.Calendar
import com.lasic.cloud._
import se.scalablesolutions.akka.actor.Actor._
import com.lasic.cloud.VM

/**
 * Created by IntelliJ IDEA.
 * User: lmonson
 * Date: Jul 19, 2010
 * Time: 3:27:38 PM
 * To change this template use File | Settings | File Templates.
 */

class MockVolume(val id:String, config:VolumeConfiguration ) extends Volume with Logging{
  var state = VolumeState.Available
  val createTime = Calendar.getInstance
  var attachInfo:VolumeAttachmentInfo = null

  def info:VolumeInfo = {
    if ( state==VolumeState.Unknown )
      throw new Exception("volume doesn't exist")
    new VolumeInfo(id, config.size, config.snapID, config.availabilityZone, state)
  }

  def delete = {
    state = VolumeState.Deleting
    spawn {
      Thread.sleep(2000)
      state = VolumeState.Unknown
    }
  }

  def attachTo(vm:VM, deviceName:String) {
    // Ensure we only attach to MockVM!
    val theVM = vm.asInstanceOf[MockVM]
    state = VolumeState.InUse
    attachInfo = new VolumeAttachmentInfo(theVM.instanceId, deviceName)
  }
}
