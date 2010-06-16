package com.lasic.model

/**
 * 
 * @author Brian Pugh
 */

class VolumeInstance (parentInstance:NodeInstance,val name: String, val volSize: String, val device: String, val mount: String) extends Pathable {
  def parent = parentInstance
  def path = { parentInstance.path + "['%s']".format(name) }
  def children = List()


}