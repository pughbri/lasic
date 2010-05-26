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

  val cloud: Cloud = cloudInst
  var machineState = MachineState.Unknown

  //todo: I don't need this... just send the MachineState.RUNNING object
  private val RUNNING="RUNNING"

  override def startup() {
    withDelay(super.startup())
    machineState = MachineState.Pending
    start()
    this ! "start timer"
  }

  override def reboot() {
    machineState = MachineState.Rebooting
    withDelay(super.reboot())
    machineState = MachineState.Pending

    this ! "start timer"
  }

  override def shutdown() {
    //todo: make the transition from shuttingdown to terminated non-blocking (like pending to running)
    machineState = MachineState.ShuttingDown
    withDelay(super.shutdown())
    machineState = MachineState.Terminated
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
      react{
        case RUNNING =>
          machineState = MachineState.Running
        case msg => setServerRunningWithDelay(delay)
      }
    }
  }

  def setServerRunningWithDelay(delay: int) {
    val mainActor = self
    actor {
      Thread.sleep(delay * 1000)
      mainActor ! RUNNING
    }
  }

}