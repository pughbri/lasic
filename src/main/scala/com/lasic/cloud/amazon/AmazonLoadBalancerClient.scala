package com.lasic.cloud.amazon

import collection.JavaConversions._
import com.lasic.cloud.LoadBalancerClient
import com.amazonaws.services.elasticloadbalancing.AmazonElasticLoadBalancingClient
import com.amazonaws.services.elasticloadbalancing.model.{DeleteLoadBalancerRequest, Listener, CreateLoadBalancerRequest}
import com.lasic.util.Logging
import java.lang.String

/**
 * 
 * @author Brian Pugh
 */

class AmazonLoadBalancerClient(awsLoadBalancingClient: AmazonElasticLoadBalancingClient) extends LoadBalancerClient with Logging{

  def createLoadBalancer(name: String, lbPort: Int, instancePort: Int, protocol: String, sslCertificateId: String, availabilityZones: List[String]) : String = {
    val lbRequest = new CreateLoadBalancerRequest().withLoadBalancerName(name)
    lbRequest.setAvailabilityZones(availabilityZones)
    val listener = new Listener().withLoadBalancerPort(lbPort).withInstancePort(instancePort).withProtocol(protocol)
    lbRequest.setListeners(List(listener))
    val dnsName = awsLoadBalancingClient.createLoadBalancer(lbRequest).getDNSName

    logger.debug("created load balancer " + name + ". dns is " + dnsName)
    
    dnsName
  }


  def deleteLoadBalancer(name: String) {
    logger.debug("deleting load balancer: " + name)

    val deleteRequest = new DeleteLoadBalancerRequest().withLoadBalancerName(name)
    awsLoadBalancingClient.deleteLoadBalancer(deleteRequest)
  }
}