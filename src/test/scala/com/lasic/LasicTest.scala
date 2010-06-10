package com.lasic

;

import junit.framework._;

/**
 * Unit test for simple Lasic.
 */
class LasicTest extends TestCase("lasic") {

  def testDeploy() = {
    val sourceFileURL = classOf[Application].getResource("/parser/Program201.lasic")
    Lasic.runLasic(Array("-c=mock", sourceFileURL.getPath))
  }

  def testParseArgs() = {
    Lasic.parseArgs(Array("-c=aws", "myfile"))
    assert(Lasic.cloudProvider == Lasic.CloudProvider.Amazon, "Expected " + Lasic.CloudProvider.Amazon + " got " + Lasic.cloudProvider)
    assert("myfile" == Lasic.lasicFile, "Expected myfile got " + Lasic.lasicFile)

    Lasic.parseArgs(Array("--cloud=mock"))
    assert(Lasic.cloudProvider == Lasic.CloudProvider.Mock, "Expected " + Lasic.CloudProvider.Mock + " got " + Lasic.cloudProvider)
  }


}
