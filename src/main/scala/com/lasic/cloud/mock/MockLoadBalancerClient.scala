package com.lasic.cloud.mock

import com.lasic.cloud.LoadBalancerClient
import com.lasic.util.Logging

/**
 *
 * @author Brian Pugh
 */

object MockLoadBalancerClient extends LoadBalancerClient with Logging {
  case class InternalLoadBalancerInst (name: String, lbPort: Int,instancePort: Int, protocol: String, sslcertificate: String)

  private var loadBalancers = List[InternalLoadBalancerInst]()
  private val lock = "lock"

  def createLoadBalancer(name: String, lbPort: Int, instancePort: Int, protocol: String, sslCertificateId: String, availabilityZones: List[String]) = {
    logger.info("creating load balancer: " + name)
    val lb = new InternalLoadBalancerInst(name, lbPort, instancePort, protocol, sslCertificateId)
    lock.synchronized {
      loadBalancers ::= lb
    }
    name + ".mockdns.com"
  }


  def deleteLoadBalancer(name: String) = {
    lock synchronized {
      loadBalancers = loadBalancers filter (_.name != name) 
    }
  }

  def reset() {
    lock.synchronized {
      loadBalancers = List[InternalLoadBalancerInst]()
    }
  }

    def getLoadBalancers = {
    lock.synchronized {
      loadBalancers
    }
  }
}