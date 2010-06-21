package com.lasic

;

import junit.framework._


/**
 * Unit test for simple Lasic.
 */
class LasicTest extends TestCase("lasic") {
  override def setUp = {
    Lasic.lasicFile = null
    Lasic.verbArg = null
    Lasic.cloudProvider = Lasic.CloudProvider.Amazon
  }

  def getLasicFilePath(num: Int) = {
    classOf[Application].getResource("/parser/Program" + num + ".lasic").getPath()
  }

  def testDeployWithMock() = {
    Lasic.runLasic(Array("-c=mock", "deploy", getLasicFilePath(201)))
  }

  def testDeployWithAmazon() = {
    if (false) {
      //in order to run this test you need to
      //1) put real aws keys in src/test/lasic.properties (AWS_ACCESS_KEY and AWS_SECRET_KEY)
      //2) put a "default" key for your account in ~/.lasic/default.pem
      //NOTE: the test does NOT shutdown the instance.  You need to shut it down manually after you run the test
      Lasic.runLasic(Array("-c=aws", "deploy", getLasicFilePath(201)))
    }
  }

  def testRunScriptWithMock() = {
    Lasic.runLasic(Array("-c=mock", "runAction", getLasicFilePath(201)))
  }

  def testParseArgs() = {

    Lasic.parseArgs(Array("snapshot", "myscript.lasic"))
    assert(Lasic.cloudProvider == Lasic.CloudProvider.Amazon, "Expected " + Lasic.CloudProvider.Amazon + " got " + Lasic.cloudProvider)
    assert("myscript.lasic" == Lasic.lasicFile, "Expected myscript.lasic got " + Lasic.lasicFile)
    assert("snapshot" == Lasic.verbArg, "Expected snapshot got " + Lasic.verbArg)

    Lasic.lasicFile = null
    Lasic.verbArg = null

    Lasic.parseArgs(Array("--cloud=mock", "deploy", "someDeploy.lasic"))
    assert(Lasic.cloudProvider == Lasic.CloudProvider.Mock, "Expected " + Lasic.CloudProvider.Mock + " got " + Lasic.cloudProvider)
    assert("someDeploy.lasic" == Lasic.lasicFile, "Expected someDeploy.lasic got " + Lasic.lasicFile)
    assert("deploy" == Lasic.verbArg, "Expected deploy got " + Lasic.verbArg)

  }


}
