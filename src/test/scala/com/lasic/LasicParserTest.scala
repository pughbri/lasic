package com.lasic

import junit.framework._
import parser.{LasicCompiler, LasicParser};
import org.apache.commons.io.IOUtils
import parser.ast._

/**
 * Created by IntelliJ IDEA.
 * User: lmonson
 * Date: May 7, 2010
 * Time: 9:38:00 PM
 * To change this template use File | Settings | File Templates.
 */

class LasicParserTest extends TestCase("LasicParserTest") {
  def getLasicProgram(i: Int) = {
    val path = "/parser/Program%03d.lasic".format(i)
    val is = getClass.getResourceAsStream(path)
    val program = IOUtils.toString(is);
    LasicCompiler.compile(program)
  }

  def assertEquals(a: Any, b: Any) = {
    assert(a == b)
  }

  /**
   * Ensures that comments are allowed as whitespace in a lasic program
   */
  def testComments() = {
    val program = getLasicProgram(1);
  }

  def testEmptySystem() = {
    val program = getLasicProgram(2);
    assertEquals(2, program.count)
    assertEquals(2, program.instances.size)
    assertEquals(program, program.instances(0).parent)
    assertEquals(program, program.instances(1).parent)
    assertEquals(0, program.instances(0).nodegroups.size)
    assertEquals(0, program.instances(1).nodegroups.size)

  }
  def testVariableSubstitution() = {
    val program = getLasicProgram(3);
    assertEquals("sysvar", program.name)
    assertEquals("var2", program.instances(0).nodegroups(0).name)
  }
  /**
   * Rigourous Tests :-)
   */
  def testSimpleProgram() = {
    val program = getLasicProgram(100);

    assertEquals("sys", program.name)
    assertEquals(2, program.count)
    assertEquals(2, program.instances.size)

    // system instance 0 should have one nodegroup, with 3 instances in it
    for (i <- 0 to 1) {
      assertEquals(1, program.instances(i).nodegroups.size)
      assertEquals("a node", program.instances(i).nodegroups(0).name)
      assertEquals(3, program.instances(i).nodegroups(0).count)
      assertEquals(3, program.instances(i).nodegroups(0).instances.size)
      assertEquals("machineimage", program.instances(i).nodegroups(0).machineimage)
      assertEquals("kernelid", program.instances(i).nodegroups(0).kernelid)
      assertEquals("ramdiskid", program.instances(i).nodegroups(0).ramdiskid)
      assertEquals(1, program.instances(i).nodegroups(0).groups.size)
      assertEquals(List("group"), program.instances(i).nodegroups(0).groups)
      assertEquals("key", program.instances(i).nodegroups(0).key)
      assertEquals("user", program.instances(i).nodegroups(0).user)
      assertEquals("small", program.instances(i).nodegroups(0).instancetype)
      // test scripts
      // test scp
    }




    assertEquals(1, program.subsystems.size )
    assertEquals(List("subsystem 1"), program.subsystems.toList.map {x => x.name})
    assertEquals(1, program.subsystems(0).instances.size )
    assertEquals(0, program.subsystems(0).instances(0).nodegroups.size )
    
    /*
        assertEquals("sys", ast.name)
        assertEquals(2, ast.count)
        assertEquals(1, ast.nodes.size)
        assertEquals(List("a node"), ast.nodes.toList.map { x => x.name} )
        val node = ast.nodes(0)
        assertEquals("a node", node.name )
        assertEquals(3, node.count)
        assertEquals("machineimage", node.machineimage)
        assertEquals("kernelid", node.kernelid)
        assertEquals("ramdiskid",  node.ramdiskid)
        assertEquals(1, node.groups.size)
        assertEquals(List("group"), node.groups)
        assertEquals("key", node.key)
        assertEquals("user", node.user)
        assertEquals("small", node.instancetype)

        // test scripts
        // test scp

        assertEquals(1, ast.subsystems.size)
        assertEquals(List("subsystem 1"), ast.subsystems.toList.map { x => x.name} )
        val subsys = ast.subsystems(0)
        assertEquals(1, subsys.count)
    */

  }
  //    def testKO() = assertTrue(false);

  /*
 system  "sys" {
props {
  count: 2
}

  node "a node" {
      props {
          count: 3
          machineimage: "machineimage"
          kernelid: "kernelid"
          ramdiskid:        "ramdiskid"
          groups:            "group"
          key:              "key"
          user:             "user"
          instancetype:     "small"
      }

  scripts {
    "some_script": {}
    "another": {
      foo:"bar"
    }
  }

      scp {
    "src":"dest"
    "src2":"dest2"
      }

  }


  system "subsystem 1" {}
}
  */


}