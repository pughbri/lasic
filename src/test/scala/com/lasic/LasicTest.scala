package com.lasic;

import junit.framework._;
import Assert._;

object LasicTest {
    def suite: Test = {
        val suite = new TestSuite(classOf[LasicTest]);
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
    def testOK() = assertTrue(true);
//    def testKO() = assertTrue(false);
    

}
