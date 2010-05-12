package com.lasic.cloud

import amazon.AmazonVM
import java.io.File
import com.lasic.{LasicProperties, VM, Cloud}
import java.lang.String
import com.xerox.amazonws.ec2.{ReservationDescription, Jec2}
import com.xerox.amazonws.ec2.ReservationDescription
import java.util.Iterator


/**
 * Created by IntelliJ IDEA.
 * User: pughbc
 * Date: May 10, 2010
 * Time: 12:45:11 PM
 * To change this template use File | Settings | File Templates.
 */

class AmazonCloud extends Cloud {

  val ec2: Jec2 = {
    val key: String = LasicProperties.getProperty("AWS_ACCESS_KEY")
    val secret: String = LasicProperties.getProperty("AWS_SECRET_KEY")
    if (key == null || secret == null)
      throw new Exception("must provide both ACCESS_KEY and SECRET_KEY in properties file")
    new Jec2(key, secret);
  }

  override def createVMs(launchConfig: LaunchConfiguration, numVMs: Int, startVM: Boolean): Array[VM] = {
    createVMs(numVMs, startVM, () => new AmazonVM(this, launchConfig))
  }

  def start(vms: Array[VM]) {
    //todo: don't just iterate.  Batching things together and making a single call with params is MUCH more efficient
    vms.foreach(vm => {
      //todo: why does an import of com.xerox.amazonwx.ec2.LanuchConfiguration not work!!!
      val amazonLC = new com.xerox.amazonws.ec2.LaunchConfiguration(vm.launchConfiguration.machineImage, 1, 1)
      val rd: ReservationDescription = ec2.runInstances(amazonLC)
      println(rd.getReservationId())

      //todo: how do I cleanly deal with java collections?
//      val instances = List(rd.getInstances())
//      instances.foreach(desc => println(desc.getInstanceId()))

      val iterator: Iterator[ReservationDescription#Instance] = rd.getInstances().iterator()
      while (iterator.hasNext()) {
        val instanceId: String = iterator.next().getInstanceId
        println(instanceId)
        vm.instanceId =  instanceId;
      }

    }
      )

  }

  def reboot(vms: Array[VM]) {
    val vm: AmazonVM = new AmazonVM(this, new LaunchConfiguration())
    println(vm.lc)
  }

  def terminate(vms: Array[VM]) {
        vms.foreach(vm => {
          println("termination " + vm.instanceId)
          var instances = new java.util.ArrayList[String]
          instances.add(vm.instanceId)
          ec2.terminateInstances(instances)
        }
          )
  }

  def copyTo(vms: Array[VM], sourceFile: File, destinationAbsPath: String) {

  }

  def execute(vms: Array[VM], executableAbsPath: String) {

  }


}