package com.lasic

;

import cloud.LaunchConfiguration
import cloud.mock.MockCloud
import junit.framework._
import java.io.File
import org.scalatest.junit.AssertionsForJUnit


/**
 * Unit test for simple Lasic.
 */
class LasicTest extends TestCase("lasic") with AssertionsForJUnit {
  override def setUp = {
    System.setProperty("properties.file", new File(classOf[Application].getResource("/lasic.properties").toURI()).getCanonicalPath())
    new MockCloud().getScalingGroupClient.reset()
    new MockCloud().getLoadBalancerClient.reset()
  }

  def getLasicFilePath(num: Int) = {
    new File(classOf[Application].getResource("/parser/Program" + num + ".lasic").toURI()).getCanonicalPath()
  }

  def testDeploy() = {
    Lasic.runLasic(Array("-c", "mock", "deploy", getLasicFilePath(201)), System.out)
    val lbClient = new MockCloud().getLoadBalancerClient
    val lbMappings = lbClient.getLoadBalancerMappings
    assert(lbMappings.size == 1, "should only be 1 instance mapping to load balancer mapping")
    lbMappings foreach {
      t2 => assert(t2._2 == "test-lb")
    }
  }

  def testDeployScaleGroup() = {
    Lasic.runLasic(Array("-c", "mock", "deploy", getLasicFilePath(203)), System.out)
    val scalingGroup = new MockCloud().getScalingGroupClient
    assert(scalingGroup.getScaleGroups.size === 1)
    assert(scalingGroup.getScaleGroups(0).name.startsWith("www-lasic-webapp"))
    assert(scalingGroup.getScaleGroups(0).triggers.size === 1)
    assert(scalingGroup.getScaleGroups(0).triggers(0).breachDuration === 300)
    assert(scalingGroup.getScaleGroups(0).lbNames.size == 2)
    assert(scalingGroup.getScaleGroups(0).lbNames(0).startsWith("www-lasic-lb"))
    assert(scalingGroup.getScaleGroups(0).lbNames(1).startsWith("www-lasic-lb"))
  }

  def testDeployLoadBalancer() = {
    Lasic.runLasic(Array("-c", "mock", "deploy", getLasicFilePath(203)), System.out)
    val lbClient = new MockCloud().getLoadBalancerClient
    assert(lbClient.getLoadBalancers.size == 2)
    lbClient.getLoadBalancers foreach {
      lb =>
      if (lb.name.startsWith("www-lasic-lb-1")) {
        assert(lb.lbPort === 81)
        assert(lb.instancePort === 82)
        assert(lb.protocol === "HTTPS")
        assert(lb.sslcertificate === "someid")
      }
      else if (lb.name.startsWith("www-lasic-lb-2")) {
        assert(lb.lbPort === 90)
        assert(lb.instancePort === 91)
        assert(lb.protocol === "HTTP")
      }
      else {
        fail("unexpected loadbalancer found: " + lb.name)
      }
    }
  }

  def testDeployWithElasticIps() = {
    Lasic.runLasic(Array("-c", "mock", "deploy", getLasicFilePath(101)), System.out)
  }

  def testRunActionWithElasticIps() = {
    Lasic.runLasic(Array("-c", "mock", "-a", "assignips", "runAction", getLasicFilePath(101)), System.out)
  }

  def testRunActionWithScaleGroup() = {
    //create the "original scale group" that will be replaced
    val scalingGroup = new MockCloud().getScalingGroupClient
    val lc = new LaunchConfiguration
    lc.name = "orig-my-app-launchconfig-2010-08-23-14-30-12"
    scalingGroup.createScalingLaunchConfiguration(lc)
    scalingGroup.createScalingGroup("orig-my-app-2010-08-23-14-30-12", lc.name, 3, 5, null, null)

    //run the action
    Lasic.runLasic(Array("-c", "mock", "-a", "switchScaleGroup", "runAction", getLasicFilePath(102)), System.out)

    //ensure that the original scale group is gone, and the new one is there
    assert(scalingGroup.getScaleGroups.size === 1)
    assert(scalingGroup.getScaleGroups(0).name.startsWith("my-app"))
    assert(scalingGroup.getScaleGroups(0).triggers.size === 1)
    assert(scalingGroup.getScaleGroups(0).triggers(0).breachDuration === 300)
  }

  def testShutdown() = {
    val cloud: MockCloud = new MockCloud()

    //setup and "existing system"
    val scalingGroup = cloud.getScalingGroupClient
    val lc = new LaunchConfiguration
    lc.name = "www-lasic-webapp-01-launchconfig"
    scalingGroup.createScalingLaunchConfiguration(lc)
    scalingGroup.createScalingGroup("www-lasic-webapp-01", lc.name, 3, 5, null, null)
    val lbClient = cloud.getLoadBalancerClient
    lbClient.createLoadBalancer("www-elastic-lb-2010-10-21-12-01-46",80,80,"HTTP","",List())
    assert(scalingGroup.getScaleGroups.size === 1)
    assert(lbClient.getLoadBalancers.size === 1)

    //run bound script to shut it down
    Lasic.runLasic(Array("-c", "mock", "shutdown", getLasicFilePath(204)), System.out)

    //validate everything is gone
    assert(scalingGroup.getScaleGroups.size === 0)
    assert(lbClient.getLoadBalancers.size === 0)
  }


  def testDeployWithAmazon() = {
    if (false) {
      //in order to run this test you need to
      //1) put real aws keys in src/test/lasic.properties (AWS_ACCESS_KEY and AWS_SECRET_KEY)
      //2) put a "default" key for your account in ~/.lasic/default.pem
      //NOTE: the test does NOT shutdown the instance.  You need to shut it down manually after you run the test
      Lasic.runLasic(Array("-c", "aws", "deploy", getLasicFilePath(201)), System.out)
    }
  }

  def testParseArgs() = {

    val cmdLineArgs = Lasic.parseArgs(Array("snapshot", "myscript.lasic"))
    assert(cmdLineArgs.cloud == "aws", "Expected aws got " + cmdLineArgs.cloud)
    assert("myscript.lasic" == cmdLineArgs.verbAndScript.get(1), "Expected myscript.lasic got " + cmdLineArgs.verbAndScript.get(1))
    assert("snapshot" == cmdLineArgs.verbAndScript.get(0), "Expected snapshot got " + cmdLineArgs.verbAndScript.get(0))


    val cmdLineArgs2 = Lasic.parseArgs(Array("--cloud", "mock", "deploy", "someDeploy.lasic"))
    assert(cmdLineArgs2.cloud == "mock", "Expected mock got " + cmdLineArgs2.cloud)
    assert("someDeploy.lasic" == cmdLineArgs2.verbAndScript.get(1), "Expected someDeploy.lasic got " + cmdLineArgs2.verbAndScript.get(1))
    assert("deploy" == cmdLineArgs2.verbAndScript.get(0), "Expected deploy got " + cmdLineArgs2.verbAndScript.get(0))
  }


}
