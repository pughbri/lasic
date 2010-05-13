package com.lasic;

import junit.framework._;


object LasicTest {
    def suite: Test = {
        val suite = new TestSuite(classOf[LasicTest]);
        suite.addTestSuite(classOf[LasicCompilerTest])
        suite
    }

    def main(args : Array[String]) {
        junit.textui.TestRunner.run(suite);
    }
}

/**
 * Unit test for simple Lasic.
 */
class LasicTest extends TestCase("lasic") {

    /**
     * Rigourous Tests :-)
     */
    def testOK() = assert(true);
//    def testKO() = assertTrue(false);


    

}
