package com.lasic

import junit.framework._;
import org.apache.commons.io.IOUtils
import parser.LasicParser

/**
 * Created by IntelliJ IDEA.
 * User: lmonson
 * Date: May 7, 2010
 * Time: 9:38:00 PM
 * To change this template use File | Settings | File Templates.
 */

class LasicParserTest extends TestCase("LasicParserTest") {
  def getLasicProgram(i: Int): String = {
    val path = "/parser/Program%03d.lasic".format(i)
    val is = getClass.getResourceAsStream(path)
    IOUtils.toString(is)
  }

  /**
   * Rigourous Tests :-)
   */
  def testParser() = {
    val p = new LasicParser
    for (i <- 1 to 1) {
      val s = getLasicProgram(1);
      var result = p.parseAll(p.system, s);
      var ok = false;
      result match {
        case  p.Success(r,_) => ok = true
      }
      assert(ok, "test %d".format(i))

    }

  }
  //    def testKO() = assertTrue(false);




}