package com.lasic.model

import com.lasic.cloud.{VolumeState, Volume}
import com.lasic.cloud.noop.NoopVolume

/**
 * 
 * @author Brian Pugh
 */

class VolumeInstance (val parentNodeInstance:NodeInstance,val name: String, val volSize: Int, val device: String, val mount: String) extends Pathable {
  var volume:Volume = new NoopVolume()
  
  def parent = parentNodeInstance
  def path = { parentNodeInstance.path + "/volume['%s']".format(name) }
  def children = List()



}