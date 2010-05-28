package com.lasic.cloud.mock

import java.io.File
import com.lasic.{Cloud, VM}
import java.lang.String
import com.lasic.cloud.{MachineState, LaunchConfiguration}
import scala.actors.Actor._
import actors.Actor


/**
 * User: Brian Pugh
 * Date: May 10, 2010
 */

class MockVM(delay: Int, val launchConfiguration: LaunchConfiguration, cloudInst: Cloud) extends VM with Actor {
  def this(cloud: Cloud) = this (2, null, cloud)

  def this(delay: Int, cloud: Cloud) = this (delay, null, cloud)

  case class StateChange(state: MachineState.Value, delay: Int)
  
  val cloud: Cloud = cloudInst
  var machineState = MachineState.Unknown

  override def startup() {
    start()
    this ! StateChange(MachineState.Pending, 0)
    withDelay(super.startup())
    this ! StateChange(MachineState.Running, delay)
  }

  override def reboot() {
    this ! StateChange(MachineState.Rebooting, 0)
    withDelay(super.reboot())
    this ! StateChange(MachineState.Pending, 0)

    this ! StateChange(MachineState.Running, delay)
  }

  override def shutdown() {
    this ! StateChange(MachineState.ShuttingDown,0)
    withDelay(super.shutdown())
    this ! StateChange(MachineState.Terminated,0)
  }

  override def copyTo(sourceFile: File, destinationAbsPath: String) {
    withDelay(println("copying file " + sourceFile.getAbsoluteFile + " to " + destinationAbsPath))
  }

  override def execute(executableAbsPath: String) {
    withDelay(println("executing " + executableAbsPath))
  }


  override def getState() = machineState

  private def withDelay(callback: => Unit): Unit = {
    Thread.sleep(delay * 1000)
    callback
  }


  def act() {
    loop {
      react {
        case StateChange(state, 0) => machineState = state
        case StateChange(state, delaySecs) => setServerRunningWithDelay(state, delay)
        case state: MachineState.Value => machineState = state
      }
    }
  }

  def setServerRunningWithDelay(state: MachineState.Value, delay: Int) {
    val mainActor = self
    actor {
      Thread.sleep(delay * 1000)
      mainActor ! state
    }
  }

}