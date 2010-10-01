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
    LasicProperties.propFilename = new File(classOf[Application].getResource("/lasic.properties").toURI()).getCanonicalPath()
    new MockCloud().getScalingGroup.reset()
  }

  def getLasicFilePath(num: Int) = {
    new File(classOf[Application].getResource("/parser/Program" + num + ".lasic").toURI()).getCanonicalPath()
  }

  def testDeployWithMock() = {
    Lasic.runLasic(Array("-c", "mock", "deploy", getLasicFilePath(201)))
  }

  def testDeployScaleGroupWithMock() = {
    Lasic.runLasic(Array("-c", "mock", "deploy", getLasicFilePath(203)))
  }

  def testDeployWithElasticIps() = {
    Lasic.runLasic(Array("-c", "mock", "deploy", getLasicFilePath(101)))
  }

  def testRunActionWithElasticIps() = {
    Lasic.runLasic(Array("-c", "mock", "-a", "assignips", "runAction", getLasicFilePath(101)))
  }

  def testRunActionWithScaleGroup() = {
    //create the "original scale group" that will be replaced
    val scalingGroup = new MockCloud().getScalingGroup
    val lc = new LaunchConfiguration
    lc.name = "orig-my-app-launchconfig-2010-08-23-14-30-12"
    scalingGroup.createScalingLaunchConfiguration(lc)
    scalingGroup.createScalingGroup("orig-my-app-2010-08-23-14-30-12", lc.name, 3, 5, null)

    //run the action
    Lasic.runLasic(Array("-c", "mock", "-a", "switchScaleGroup", "runAction", getLasicFilePath(102)))

    //ensure that the original scale group is gone, and the new one is there
    assert(scalingGroup.getScaleGroups.size === 1)
    assert(scalingGroup.getScaleGroups(0).name.startsWith("my-app"))
    assert(scalingGroup.getScaleGroups(0).triggers.size === 1)
    assert(scalingGroup.getScaleGroups(0).triggers(0).breachDuration === 300)
  }

  def testShutdown() = {
    val scalingGroup = new MockCloud().getScalingGroup
    val lc = new LaunchConfiguration
    lc.name = "www-lasic-webapp-01-launchconfig"
    scalingGroup.createScalingLaunchConfiguration(lc)
    scalingGroup.createScalingGroup("www-lasic-webapp-01", lc.name, 3, 5, null)

    Lasic.runLasic(Array("-c", "mock", "shutdown", getLasicFilePath(204)))
    assert(scalingGroup.getScaleGroups.size === 0)
  }


  def testDeployWithAmazon() = {
    if (false) {
      //in order to run this test you need to
      //1) put real aws keys in src/test/lasic.properties (AWS_ACCESS_KEY and AWS_SECRET_KEY)
      //2) put a "default" key for your account in ~/.lasic/default.pem
      //NOTE: the test does NOT shutdown the instance.  You need to shut it down manually after you run the test
      Lasic.runLasic(Array("-c", "aws", "deploy", getLasicFilePath(201)))
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
