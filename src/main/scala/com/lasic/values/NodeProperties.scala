package com.lasic.values

trait NodeProperties {
  private var nameVar: Option[String] = None
  private var countVar: Option[Int] = None
  private var machineimageVar: Option[String] = None
  private var kernelidVar: Option[String] = None
  private var ramdiskidVar: Option[String] = None
  private var groupsVar: Option[List[String]] = None
  private var keyVar: Option[String] = None
  private var userVar: Option[String] = None
  private var userDataVar: Option[String] = None
  private var instancetypeVar: Option[String] = None

  def name: String = {
    nameVar match {
      case Some(x) => x
      case None => ""
    }
  }

  def name_=(name: String) {
    nameVar = new Some(name)
  }

  def count: Int = {
    countVar match {
      case Some(x) => x
      case None => 1
    }
  }

  def count_=(count: Int) {
    countVar = new Some(count)
  }

  def machineimage: String = {
    machineimageVar match {
      case Some(x) => x
      case None => null
    }
  }

  def machineimage_=(machineimage: String) {
    machineimageVar = new Some(machineimage)
  }


  def kernelid: String = {
    kernelidVar match {
      case Some(x) => x
      case None => null
    }
  }

  def kernelid_=(kernelid: String) {
    kernelidVar = new Some(kernelid)
  }

  def ramdiskid: String = {
    ramdiskidVar match {
      case Some(x) => x
      case None => null
    }
  }

  def ramdiskid_=(ramdiskid: String) {
    ramdiskidVar = new Some(ramdiskid)
  }

  def groups: List[String] = {
    groupsVar match {
      case Some(x) => x
      case None => List()
    }
  }

  def groups_=(groups: List[String]) {
    groupsVar = new Some(groups)
  }

  def key: String = {
    keyVar match {
      case Some(x) => x
      case None => null
    }
  }

  def key_=(key: String) {
    keyVar = new Some(key)
  }

  def user: String = {
    userVar match {
      case Some(x) => x
      case None => null
    }
  }

  def user_=(user: String) {
    userVar = new Some(user)
  }

  def data: String = {
    userDataVar match {
      case Some(x) => x
      case None => null
    }
  }

  def data_=(userData: String) {
    userDataVar = new Some(userData)
  }

  def instancetype: String = {
    instancetypeVar match {
      case Some(x) => x
      case None => null
    }
  }

  def instancetype_=(instancetype: String) {
    instancetypeVar = new Some(instancetype)
  }

  def copySetProperties(that: NodeProperties) {
    that.nameVar match {
      case Some(x) => nameVar = that.nameVar
      case None =>
    }
    that.countVar match {
      case Some(x) => countVar = that.countVar
      case None =>
    }
    that.machineimageVar match {
      case Some(x) => machineimageVar = that.machineimageVar
      case None =>
    }
    that.kernelidVar match {
      case Some(x) => kernelidVar = that.kernelidVar
      case None =>
    }
    that.ramdiskidVar match {
      case Some(x) => ramdiskidVar = that.ramdiskidVar
      case None =>
    }
    that.groupsVar match {
      case Some(x) => groupsVar = that.groupsVar
      case None =>
    }
    that.keyVar match {
      case Some(x) => keyVar = that.keyVar
      case None =>
    }
    that.userVar match {
      case Some(x) => userVar = that.userVar
      case None =>
    }
    that.userDataVar match {
      case Some(x) => userDataVar = that.userDataVar
      case None =>
    }
    that.instancetypeVar match {
      case Some(x) => instancetypeVar = that.instancetypeVar
      case None =>
    }
  }

}