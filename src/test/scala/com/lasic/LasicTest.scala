package com.lasic

;

import cloud.amazon.AmazonCloud
import junit.framework._
import java.io.File


/**
 * Unit test for simple Lasic.
 */
class LasicTest extends TestCase("lasic") {

  def getLasicFilePath(num: Int) = {
    new File(classOf[Application].getResource("/parser/Program" + num + ".lasic").toURI()).getCanonicalPath()
  }

  def testDeployWithMock() = {
    Lasic.runLasic(Array("-c", "mock", "deploy2", getLasicFilePath(201)))
  }

  def testDeployScaleGroupWithMock() = {
    Lasic.runLasic(Array("-c", "mock", "deploy2", getLasicFilePath(203)))
  }

  def testDeployWithElasticIps() = {
    Lasic.runLasic(Array("-c", "mock", "deploy2", getLasicFilePath(101)))
  }

  def testRunActionWithElasticIps() = {
    Lasic.runLasic(Array("-c", "mock", "-a", "assignips", "runAction2", getLasicFilePath(101)))
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
