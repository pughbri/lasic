package com.lasic.cloud

/**
 * 
 * @author Brian Pugh
 */

trait LoadBalancerClient {

  def createLoadBalancer(name: String, lbPort: Int, instancePort: Int, protocol: String, sslCertificateId: String, availabilityZones: List[String]): String

  def deleteLoadBalancer(name: String)

  def registerWith(loadBalancer: String, vm: VM)

}