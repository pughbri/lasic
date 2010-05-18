package com.lasic.cloud

import amazon.AmazonVM
import com.lasic.{LasicProperties, VM, Cloud}
import java.lang.String
import com.xerox.amazonws.ec2.{Jec2}
import com.xerox.amazonws.ec2.ReservationDescription
import java.util.{List, Iterator}

/**
 * User: Brian Pugh
 * Date: May 10, 2010
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
    createVMs(numVMs, startVM) {new AmazonVM(this, launchConfig)}
  }


  def start(vms: Array[VM]) {
    //todo: don't just iterate.  Batching things together and making a single call with params is MUCH more efficient
    vms.foreach(vm => {startVM(vm)})
  }

  private def startVM(vm: VM) {
    val amazonLC = createLaunchConfiguration(vm.launchConfiguration)
    val rd: ReservationDescription = ec2.runInstances(amazonLC)
    println(rd.getReservationId())

    //todo: how do I cleanly deal with java collections?
    //      val instances = List(rd.getInstances())
    //      instances.foreach(desc => println(desc.getInstanceId()))

    val iterator: Iterator[ReservationDescription#Instance] = rd.getInstances().iterator()
    while (iterator.hasNext()) {
      val instance: ReservationDescription#Instance = iterator.next
      val instanceId: String = instance.getInstanceId
      println(instanceId)
      vm.machineDescription = new MachineDescription(instanceId,
//        MachineState.valueOf(instance.getState).get,
        instance.getDnsName,
        instance.getPrivateDnsName)
    }
  }

  private def createLaunchConfiguration(lasicLC: LaunchConfiguration): com.xerox.amazonws.ec2.LaunchConfiguration = {
    val launchConfig = new com.xerox.amazonws.ec2.LaunchConfiguration(lasicLC.machineImage, 1, 1)
    launchConfig.setKernelId(lasicLC.kernelId)
    launchConfig.setRamdiskId(lasicLC.ramdiskId)
    launchConfig.setAvailabilityZone(lasicLC.availabilityZone)
    launchConfig.setInstanceType(lasicLC.instanceType)
    launchConfig.setKeyName(lasicLC.key);
    launchConfig
  }

  def reboot(vms: Array[VM]) {
    val vm: AmazonVM = new AmazonVM(this, new LaunchConfiguration())
    println(vm.lc)
  }

  def terminate(vms: Array[VM]) {
    vms.foreach(vm => {
      println("termination " + vm.machineDescription.instanceId)
      var instances = new java.util.ArrayList[String]
      instances.add(vm.machineDescription.instanceId)
      ec2.terminateInstances(instances)
    }
      )
  }


  def getState(vm: VM): MachineState.Value = {
    val list: List[ReservationDescription] = ec2.describeInstances(Array(vm.machineDescription.instanceId))
    if (list.size != 1) {
      throw new IllegalStateException("expected a single reservation description for instance id " + vm.machineDescription.instanceId + " but got " + list.size)
    }

    val instances: List[ReservationDescription#Instance] = list.get(0).getInstances
    if (list.size != 1) {
      throw new IllegalStateException("expected a single instance for instance id " + vm.machineDescription.instanceId + " but got " + instances.size)
    }

    MachineState.valueOf(instances.get(0).getState).get

  }
}