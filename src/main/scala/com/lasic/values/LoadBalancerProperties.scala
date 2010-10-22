package com.lasic.values

/**
 * 
 * @author Brian Pugh
 */

trait LoadBalancerProperties {
  var localName = ""
  var lbPort = 80
  var instancePort = 80
  var protocol = ""
  var sslcertificate = ""
}