package com.lasic.model

import com.lasic.values.LoadBalancerProperties

/**
 *
 * @author Brian Pugh
 */

class LoadBalancerInstance extends Pathable with LoadBalancerProperties{
  var parentSystemInstance: SystemInstance = null

  def parent = parentSystemInstance

  def path = {parent.path + "/load-balancer['" + localName + "']"}

  def children = List()

  var cloudName = ""
  var dnsName = ""
}