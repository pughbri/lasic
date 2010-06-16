package com.lasic

;

import junit.framework._
import java.lang.String;

/**
 * Unit test for simple Lasic.
 */
class LasicTest extends TestCase("lasic") {

  def getLasicFilePath(num: Int) = {
    classOf[Application].getResource("/parser/Program" + num + ".lasic").getPath()
  }

  def testDeployWithMock() = {
    Lasic.runLasic(Array("-c=mock", getLasicFilePath(201)))
  }

  def testDeployWithAmazon() = {
    if (false) {
      //in order to run this test you need to
      //1) put real aws keys in src/test/lasic.properties (AWS_ACCESS_KEY and AWS_SECRET_KEY)
      //2) put a "default" key for your account in ~/.lasic/default.pem
      //NOTE: the test does NOT shutdown the instance.  You need to shut it down manually after you run the test
      Lasic.runLasic(Array("-c=aws", getLasicFilePath(201)))
    }
  }

  def testParseArgs() = {
    Lasic.parseArgs(Array("-c=aws", "myfile"))
    assert(Lasic.cloudProvider == Lasic.CloudProvider.Amazon, "Expected " + Lasic.CloudProvider.Amazon + " got " + Lasic.cloudProvider)
    assert("myfile" == Lasic.lasicFile, "Expected myfile got " + Lasic.lasicFile)

    Lasic.parseArgs(Array("--cloud=mock"))
    assert(Lasic.cloudProvider == Lasic.CloudProvider.Mock, "Expected " + Lasic.CloudProvider.Mock + " got " + Lasic.cloudProvider)
  }


}
