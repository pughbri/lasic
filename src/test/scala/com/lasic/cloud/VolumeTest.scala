package com.lasic.cloud

import amazon.AmazonCloud
import junit.framework.TestCase
//import com.lasic.interpreter.actors.VolumeActor.MsgQueryState
import com.lasic.Cloud
//import com.lasic.interpreter.actors.VolumeActor
import mock.MockCloud
//import VolumeActor._
//import VolumeActor.VolumeActorState._
import se.scalablesolutions.akka.actor.{Actor, ActorRef}
import MachineState.MachineState

/**
 * A trait used to implement volume tests
 */
class VolumeTest extends TestCase("MockVolumeTest") {
  //val cloud: Cloud = new AmazonCloud()
  val cloud: Cloud = new MockCloud()
  var actor:ActorRef = null

//  def actorIsInState(x: Any) = {
//    val y = actor !! MsgQueryState
//    val result: Boolean =
//    y match {
//      case Some(something) => something == x
//      case x => false
//    }
//    result
//  }

  def testDefaultedZoneCreate() = {
    val config = new VolumeConfiguration(100, null, null)
    val volume = cloud.createVolume(config)
    assert(volume != null)
    assert(volume.info.availabilityZone != null)
    assert(volume.info.state == VolumeState.Available)
    volume.delete
    println(volume.info)

  }

//  def waitForState(max:Int, state:Any) {
//    var maxTries:Int = max
//    while (!actorIsInState(state)) {
//      maxTries = maxTries - 1
//      maxTries match {
//        case 0 => throw new Exception("failed")
//        case _ => Thread.sleep(1000)
//      }
//    }
//  }

//  def testCreateViaVolumeActor {
//    actor = Actor.actorOf(new VolumeActor(cloud)).start
//    val config = new VolumeConfiguration(1, null, null)
//    actor ! MsgCreate(config)
//    waitForState(30, Available)
//
//    actor ! MsgDelete
//    waitForState(10, Deleting)
//    //waitForState(120, Deleted)   // need a way to do this... deletion may take unbounded amounts of time, even days
//
//  }

  def testAttachToVM() {
    val volConfig = new VolumeConfiguration(1, null, null)
    val vol = cloud.createVolume(volConfig)

    val lc: LaunchConfiguration = new LaunchConfiguration
    lc.machineImage = "ami-714ba518" //base ubuntu image
    lc.key = "fhd"
    lc.userName = "ubuntu"
    lc.groups = List("default")
    val vm = cloud.createVM(lc, true)

    while( vm.getMachineState!=MachineState.Running) {
      Thread.sleep(5000)
    }
    vol.attachTo(vm,"/dev/sdh")

    assert( vol.info.state == VolumeState.InUse )
    vm.shutdown
    while( vm.getMachineState!=MachineState.Terminated) {
      Thread.sleep(5000)
    }
    vol.delete


  }


}