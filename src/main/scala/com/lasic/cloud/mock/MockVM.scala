package com.lasic.cloud.mock

import java.io.File
import com.lasic.{Cloud, VM}
import java.lang.String
import com.lasic.cloud.{MachineState, LaunchConfiguration}
import scala.actors.Actor._
import actors.Actor
import com.lasic.cloud.MachineState._
import util.Random

/**
 * User: Brian Pugh
 * Date: May 10, 2010
 */

class MockVM(delay: Int, val launchConfiguration: LaunchConfiguration, cloudInst: Cloud) extends VM with Actor {
  start()
  var isInit = false

  def this(cloud: Cloud) = this (2, null, cloud)

  def this(delay: Int, cloud: Cloud) = this (delay, null, cloud)

  case class StateChange(state: MachineState.Value, delay: Int)
  
  val cloud: Cloud = cloudInst
  var machineState:MachineState = Unknown

  override def startup() {
    withDelay(super.startup())
  }

  override def reboot() {
    withDelay(super.reboot())
  }

  override def shutdown() {
    withDelay(super.shutdown())
  }

  override def copyTo(sourceFile: File, destinationAbsPath: String) {
    withDelay(println("copying file " + sourceFile.getAbsoluteFile + " to " + destinationAbsPath))
  }

  override def execute(executableAbsPath: String) {
    withDelay(println("executing " + executableAbsPath))
  }


  override def getMachineState() = machineState

  private def withDelay(callback: => Unit): Unit = {
    Thread.sleep(delay * 1000)
    callback
  }

  override def isInitialized(): Boolean = {
    Thread.sleep(delay * 1000)
    isInit
  }

  def act() {
    loop {
      react {
        case StateChange(state, 0) => {
          assignState(state)
          //println(machineState)
        }
        case StateChange(state, delaySecs) => {
          setServerRunningWithDelay(state, delay)
          //println(machineState)
        }
        case state: MachineState.Value => {
          assignState(state)
          //println(machineState)
        }
        case ("init",initValue:Boolean,delay:Int) => setInitWithDelay(initValue,delay)
        case ("init",initValue:Boolean) => isInit = initValue
      }
    }
  }

  def assignState(s:MachineState.Value) {
    if ( instanceId==null )
      instanceId = "i-" + new Random().nextInt(5000)
    machineState = s
  }

  def setServerRunningWithDelay(state: MachineState.Value, delay: Int) {
    val mainActor = self
    actor {
      Thread.sleep(delay * 1000)
      mainActor ! state
    }
  }

  def setInitWithDelay(init:Boolean, delay:Int) {
    val mainActor = self
    actor {
      Thread.sleep(delay * 1000)
      mainActor ! ("init",init)
    }
  }
}