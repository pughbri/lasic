package com.lasic

import junit.framework._;
import org.apache.commons.io.IOUtils
import parser.LasicParser
import parser.ast._

/**
 * Created by IntelliJ IDEA.
 * User: lmonson
 * Date: May 7, 2010
 * Time: 9:38:00 PM
 * To change this template use File | Settings | File Templates.
 */

class LasicParserTest extends TestCase("LasicParserTest") {
  val tests = List(
      List(1, new ASTSystem())
    )


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
      p.parseAll(p.system,s) match {
        case  p.Success(r:ASTSystem,_) => println("Result: "+r);
        case x => println(x); assert(false, "Test %d".format(i))
      }
    }
  }
  //    def testKO() = assertTrue(false);




}