package com.lasic

import junit.framework._
import parser.{LasicCompiler};
import org.apache.commons.io.IOUtils

/**
 * Tests a variety of sample LASIC programs to ensure that they compile into the proper object model
 */
class LasicCompilerTest extends TestCase("LasicCompilerTest") {

  /**
   * Load a program, based on a test number, from the classpath
   */
  def getLasicProgram(i: Int) = {
    val path = "/parser/Program%03d.lasic".format(i)
    val is = getClass.getResourceAsStream(path)
    val program = IOUtils.toString(is);
    LasicCompiler.compile(program)
  }

  /* utility */
  def assertEquals(a: Any, b: Any) = {
    assert(a == b)
  }

  /**
   * Ensures that comments are allowed as whitespace in a lasic program
   */
  def testComments() = {
    val program = getLasicProgram(1);
  }

  /**
   * Ensure that an empty program, but with repeated "arity" produces objects
   */
  def testEmptySystem() = {
    val program = getLasicProgram(2);
    assertEquals(2, program.count)
    assertEquals(2, program.instances.size)
    assertEquals(program, program.instances(0).parent)
    assertEquals(program, program.instances(1).parent)
    assertEquals(0, program.instances(0).nodegroups.size)
    assertEquals(0, program.instances(1).nodegroups.size)

  }

  /**
   * Ensure that variable substitution is occurring
   */
  def testVariableSubstitution() = {
    val program = getLasicProgram(3);
    assertEquals("sysvar", program.name)
    assertEquals("var2", program.instances(0).nodegroups(0).name)
  }

  /**
   * Ensure that SCP statements are parsed
   */
  def testScp() = {
    val program= getLasicProgram(4);
    assertEquals(2, program.instances(0).nodegroups(0).scpMap.size)
    assertEquals("dest1", program.instances(0).nodegroups(0).scpMap("src1"))
    assertEquals("dest2", program.instances(0).nodegroups(0).scpMap("src2"))
  }

  /**
   * Ensure that script statements are parsed
   */
  def testScripts() = {
    val program= getLasicProgram(5);
    assertEquals(2, program.instances(0).nodegroups(0).scriptMap.size)

    var map = program.instances(0).nodegroups(0).scriptMap("some_script")
    assertEquals(0, map.size)

    map = program.instances(0).nodegroups(0).scriptMap("another")
    assertEquals(1, map.size)
    assertEquals("bar", map("foo"))
  }

  /**
   *  Parse a basic, but non trivial, program and test a variety of features about it
   */
  def testSimpleProgram() = {
    val program = getLasicProgram(100);

    assertEquals("sys", program.name)
    assertEquals(2, program.count)
    assertEquals(2, program.instances.size)

    // system instance 0 should have one nodegroup, with 3 instances in it
    for (i <- 0 to 1) {
      assertEquals(1, program.instances(i).nodegroups.size)

      val nodeGroup = program.instances(i).nodegroups(0)

      assertEquals("a node", nodeGroup.name)
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
      val scriptMap = nodeGroup.scriptMap
      assertEquals(2,scriptMap.size)
      var map = scriptMap("some_script")
      assertEquals(0, map.size)
      map = scriptMap("another")
      assertEquals(1, map.size)
      assertEquals("bar", map("foo"))

      val scpMap = nodeGroup.scpMap
      assertEquals("dest1", scpMap("src1"))
      assertEquals("dest2", scpMap("src2"))

      // test scp
    }

    assertEquals(1, program.subsystems.size )
    assertEquals(List("subsystem 1"), program.subsystems.toList.map {x => x.name})
    assertEquals(1, program.subsystems(0).instances.size )
    assertEquals(0, program.subsystems(0).instances(0).nodegroups.size )
  }
}