package com.lasic.cloud.mock

import junit.framework.TestCase
import com.lasic.{Cloud, VM}
import com.lasic.cloud.{VolumeConfiguration, LaunchConfiguration, MachineState}
import com.lasic.interpreter.actors.VolumeActor
import com.lasic.interpreter.actors.VolumeActor.{MsgQueryState, MsgCreate}
import VolumeActor.VolumeActorState._
import se.scalablesolutions.akka.actor.{ActorRef, Actor}

/**
 *
 * User: Brian Pugh
 * Date: May 25, 2010
 */

class MockVolumeTest extends TestCase("MockVolumeTest") {

  def isInState(actor:ActorRef, x: Any) = {
    val y = actor !! MsgQueryState
    val result: Boolean =
    y match {
      case Some(something) => something == x
      case x => false
    }

    result

  }

  def testDefaultedZoneCreate() = {
    val cloud:Cloud = new MockCloud(2)
    val config = new VolumeConfiguration(100, null, null)
    val volume = cloud.createVolume(config)
    assert( volume!=null )
    assert( volume.info.availabilityZone!=null )

  }

  def testCreateViaVolumeActor {
    val cloud:Cloud = new MockCloud(2)
    val actor = Actor.actorOf(new VolumeActor(cloud)).start
    val config = new VolumeConfiguration(100, null, null )
    actor ! MsgCreate(config)
    var maxTries:Int = 100
    while( !isInState(actor,Available)) {
      maxTries = maxTries-1
      maxTries match {
        case 0 => throw new Exception("failed")
        case _ => Thread.sleep(100)
      }
    }
  }

  def testAttachToVM() {
    val cloud:Cloud = new MockCloud(2)
    val volConfig = new VolumeConfiguration(100, null, null )
    val vol = cloud.createVolume(volConfig)
    val lc: LaunchConfiguration = new LaunchConfiguration
    lc.machineImage = "ami-714ba518" //base ubuntu image
    lc.key = "default"
    lc.userName = "ubuntu"
    lc.groups = List("default")
    val vm = cloud.createVM( lc, true )
    


  }


}