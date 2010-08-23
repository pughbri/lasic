package com.lasic

;

import cloud.amazon.AmazonCloud
import cloud.mock.MockCloud
import junit.framework._
import java.io.File


/**
 * Unit test for simple Lasic.
 */
class LasicTest extends TestCase("lasic") {
  override def setUp = {
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
    Lasic.runLasic(Array("-c", "mock", "-a", "switchScaleGroup", "runAction", getLasicFilePath(102)))
    val mockScalingGroup = new MockCloud().getScalingGroup
    assert(mockScalingGroup.getScaleGroups.size == 1, "expect scaling groups size to be 1, but was " + mockScalingGroup.getScaleGroups.size)
    assert(mockScalingGroup.getScaleGroups(0).name.startsWith("my-app"))
    assert(mockScalingGroup.getScaleGroups(0).triggers.size == 1)
    assert(mockScalingGroup.getScaleGroups(0).triggers(0).breachDuration == 300)
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

  def testRunScriptWithMock() = {
    Lasic.runLasic(Array("-c", "mock", "-a", "snapshot","runAction", getLasicFilePath(202)))
  }

  def testParseArgs() = {

    val cmdLineArgs = Lasic.parseArgs(Array("snapshot", "myscript.lasic"))
    assert(cmdLineArgs.cloud == "aws", "Expected aws got " + cmdLineArgs.cloud)
    assert("myscript.lasic" == cmdLineArgs.verbAndScript.get(1), "Expected myscript.lasic got " + cmdLineArgs.verbAndScript.get(1))
    assert("snapshot" == cmdLineArgs.verbAndScript.get(0), "Expected snapshot got " + cmdLineArgs.verbAndScript.get(0))


    val cmdLineArgs2 = Lasic.parseArgs(Array("--cloud", "mock", "deploy", "someDeploy.lasic"))
    assert(cmdLineArgs2.cloud== "mock", "Expected mock got " + cmdLineArgs2.cloud)
    assert("someDeploy.lasic" == cmdLineArgs2.verbAndScript.get(1), "Expected someDeploy.lasic got " + cmdLineArgs2.verbAndScript.get(1))
    assert("deploy" == cmdLineArgs2.verbAndScript.get(0), "Expected deploy got " + cmdLineArgs2.verbAndScript.get(0))
  }


}
