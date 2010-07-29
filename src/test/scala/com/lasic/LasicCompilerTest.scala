package com.lasic

import junit.framework._
import model._
import parser.{LasicCompiler}

import org.apache.commons.io.IOUtils

/**
 * Tests a variety of sample LASIC programs to ensure that they compile into the proper object model
 */
class LasicCompilerTest extends TestCase("LasicCompilerTest") {
  override def setUp = {
    LasicProperties.propFilename = classOf[Application].getResource("/lasic.properties").getPath()
  }

  /**
   * Load a program, based on a test number, from the classpath
   */
  def getLasicProgram(i: Int) = {
    val path = "/parser/Program%03d.lasic".format(i)
    val is = getClass.getResourceAsStream(path)
    val program = IOUtils.toString(is);
    LasicCompiler.compile(program).root.children(0)
  }

  /* utility */
  def assertEquals(a: Any, b: Any) = {
    assert(a == b, "expected " + a + " got " + b)
  }

  /**
   * Ensures that comments are allowed as whitespace in a lasic program
   */
  def testComments() = {
    val program = getLasicProgram(1);
  }

  def testActionType() {
    val program = getLasicProgram(7);
    val node = program.find(("//node[*][*]"))(0).asInstanceOf[NodeInstance]
    val action = node.parent.actions(0)
    assert(action.isInstanceOf[Action], "expected com.lasic.model.Action but got " + action.getClass)

  }

  def testPathAsScriptArgument() {
    val program = getLasicProgram(7);
    val node = program.find(("//node[*][*]"))(0).asInstanceOf[NodeInstance]
    val scripts = node.parent.actions(0).scriptMap
    val args = scripts("another")
    assertEquals(2, args.size)
    assertEquals(true, args("foo").isInstanceOf[LiteralScriptArgumentValue])
    assertEquals(true, args("foo2").isInstanceOf[PathScriptArgumentValue])
    val s: String = args("foo2").literal
    assertEquals("/system['sys1']/node['node1'][0]", s);

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
    val program = getLasicProgram(4);
    assertEquals(2, program.instances(0).nodegroups(0).actions(0).scpMap.size)
    assertEquals("dest1", program.instances(0).nodegroups(0).actions(0).scpMap("src1"))
    assertEquals("dest2", program.instances(0).nodegroups(0).actions(0).scpMap("src2"))
  }

  /**
   * Ensure that script statements are parsed
   */
  def testScripts() = {
    val program = getLasicProgram(5);
    assertEquals(2, program.instances(0).nodegroups(0).actions(0).scriptMap.size)

    var map = program.instances(0).nodegroups(0).actions(0).scriptMap("some_script")
    assertEquals(0, map.size)

    map = program.instances(0).nodegroups(0).actions(0).scriptMap("another")
    assertEquals(1, map.size)
    assertEquals("bar", map("foo").literal)
  }

  def testVolumes() = {
    val program = getLasicProgram(6);
    assertEquals(2, program.instances(0).nodegroups(0).instances(0).volumes.size)

    assertEquals("node1-volume", program.instances(0).nodegroups(0).instances(0).volumes(0).name)
    assertEquals(100, program.instances(0).nodegroups(0).instances(0).volumes(0).volSize)
    assertEquals("/dev/sdh", program.instances(0).nodegroups(0).instances(0).volumes(0).device)
    assertEquals("/home/fs/lotsofdata", program.instances(0).nodegroups(0).instances(0).volumes(0).mount)

    assertEquals("node1-volume2", program.instances(0).nodegroups(0).instances(0).volumes(1).name)
    assertEquals(200, program.instances(0).nodegroups(0).instances(0).volumes(1).volSize)
    assertEquals(null, program.instances(0).nodegroups(0).instances(0).volumes(1).device)
    assertEquals(null, program.instances(0).nodegroups(0).instances(0).volumes(1).mount)

  }

  def testVolumePath {
    val program = getLasicProgram(6);
    assertEquals( "/system['sys1'][0]/node['node1'][0]/volume['node1-volume']",program.instances(0).nodegroups(0).instances(0).volumes(0).path )
    assertEquals( "/system['sys1'][0]/node['node1'][0]/volume['node1-volume2']",program.instances(0).nodegroups(0).instances(0).volumes(1).path )

    val vol1 = program.findFirst("/system['sys1'][0]/node['node1'][0]/volume['node1-volume']")
    assertEquals( vol1, program.instances(0).nodegroups(0).instances(0).volumes(0))

    val vol2 = program.findFirst("/system['sys1'][0]/node['node1'][0]/volume['node1-volume2']")
    assertEquals( vol2, program.instances(0).nodegroups(0).instances(0).volumes(1))
  }

  def testBoundPaths() = {
    val program = getLasicProgram(9);
    assertEquals("i-54adb13a", program.instances(0).nodegroups(0).instances(0).boundInstanceId)    
    assertEquals("i-54adb13b", program.instances(0).nodegroups(0).instances(1).boundInstanceId)
    assertEquals("i-54adb13c", program.instances(0).nodegroups(1).instances(0).boundInstanceId)
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
      val scriptMap = nodeGroup.actions(0).scriptMap
      assertEquals(2, scriptMap.size)
      var map = scriptMap("some_script")
      assertEquals(0, map.size)
      map = scriptMap("another")
      assertEquals(1, map.size)
      assertEquals("bar", map("foo").literal)

      val scpMap = nodeGroup.actions(0).scpMap
      assertEquals("dest1", scpMap("src1"))
      assertEquals("dest2", scpMap("src2"))

      // test scp
    }

    var inst: SystemInstance = program.instances.head
    var subsysList = inst.subsystems
    var subSys = subsysList.head
    assertEquals(List("subsystem 1"), subsysList.map {x => x.name})
    assertEquals(1, subSys.count)
    assertEquals(1, subSys.instances.size)

    inst = program.instances.tail.head
    subsysList = inst.subsystems
    subSys = subsysList.head
    assertEquals(List("subsystem 1"), subsysList.map {x => x.name})
    assertEquals(1, subSys.count)
    assertEquals(1, subSys.instances.size)

  }
}