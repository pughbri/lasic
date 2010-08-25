package com.lasic

import com.lasic.parser.LasicCompiler
import junit.framework.TestCase
import model.{ScaleGroupConfiguration, Pathable, ScaleGroupInstance}
import org.apache.commons.io.IOUtils


/**
 * Tests a variety of sample LASIC programs to ensure that they compile into the proper object model
 */
class LasicPathTest extends TestCase("LasicPathTest") {
  /**
   * Load a program, based on a test number, from the classpath
   */
  def getLasicProgram(i: Int) = {
    val path = "/paths/Program%03d.lasic".format(i)
    val is = getClass.getResourceAsStream(path)
    val program = IOUtils.toString(is);
    LasicCompiler.compile(program)
  }

  def testEmpty = {
    val program = LasicCompiler.compile("system \"foo\" {}")
    assert( program.path=="/" )
    assert( program.root.path== "/" )
    assert( program.children(0).path=="/system['foo']")
    assert( program.children(0).root.path == "/" )
    assert( program.children(0).children(0).path == "/system['foo'][0]")
  }

  def testNestedEmpty = {
    val program = LasicCompiler.compile("system \"foo\" { system \"bar\" {} }")
    assert( program.children(0).children(0).children(0).path == "/system['foo'][0]/system['bar']")
    assert( program.children(0).children(0).children(0).children(0).path == "/system['foo'][0]/system['bar'][0]")
  }

  def assertIsScaleGroup1(scaleGroup1: Pathable): Unit = {
    scaleGroup1 match {
      case scaleGroup: ScaleGroupInstance => assert(scaleGroup.localName == "grp1")
      case _ => assert(false, "expected ScaleGroupInstance but got " + scaleGroup1.getClass)
    }
  }

  def assertIsScaleGroup2(scaleGroup2: Pathable): Unit = {
    scaleGroup2 match {
      case scaleGroup: ScaleGroupInstance => assert(scaleGroup.localName == "grp2")
      case _ => assert(false, "expected ScaleGroupInstance but got " + scaleGroup2.getClass)
    }
  }

  def assertIsScaleConfig1(scaleGroupConfig1: Pathable): Unit = {
    scaleGroupConfig1 match {
      case scaleGroupConfig: ScaleGroupConfiguration => assert(scaleGroupConfig.name == "grp1-config")
      case _ => assert(false, "expected ScaleGroupConfiguration but got " + scaleGroupConfig1.getClass)
    }
  }

  def assertIsScaleConfig2(scaleGroupConfig2: Pathable): Unit = {
    scaleGroupConfig2 match {
      case scaleGroupConfig: ScaleGroupConfiguration => assert(scaleGroupConfig.name == "grp2-config")
      case _ => assert(false, "expected ScaleGroupConfiguration but got " + scaleGroupConfig2.getClass)
    }
  }

  def testQueryForScaleGroups = {
    val program = getLasicProgram(2)
    val scaleGroups = program.find("//scale-group[*]")
    assert(scaleGroups.size == 2, "expected size 2 but got " + scaleGroups.size)
    assertIsScaleGroup1(scaleGroups(0))
    assertIsScaleGroup2(scaleGroups(1))

    val scaleGroups1 = program.find("/system['sys1'][0]/scale-group['grp1']")
    assert(scaleGroups1.size == 1, "expected size 1 but got " + scaleGroups1.size)
    assertIsScaleGroup1(scaleGroups1(0))

    val scaleGroups2 = program.find("/system['sys1'][0]/scale-group['grp2']")
    assert(scaleGroups2.size == 1, "expected size 1 but got " + scaleGroups2.size)
    assertIsScaleGroup2(scaleGroups2(0))



    val scaleConfigurations = program.find("//scale-group-configuration[*]")
    assert(scaleConfigurations.size == 2)
    assertIsScaleConfig1(scaleConfigurations(0))
    assertIsScaleConfig2(scaleConfigurations(1))
  }

  def testQueryForNodes = {
    val program = getLasicProgram(1)


    val tests = List(
      ("//node[*][*]",4),
      ("//node[*]",2),      //todo: This is wrong!  ought to return 4 things, not 2
      ("//system[*][*]",2),
      ("//system[*]",2),
      ("/system['sys'][0]", 1),
      ("/system['sys']", 1),
      ("/system['sys'][0]/system['subsystem 1'][0]", 1),
      ("/system['sys'][0]/system['subsystem 1']", 1)
//      ("/system['sys'][0]/node['a']", 1),
//      ("/system['sys'][0]/node['a'][0]", 1)
//      ("/system['sys'][0]/system['subsystem 1'][0]/node['b']", 1),
//      ("/system['sys'][0]/system['subsystem 1'][0]/node['b'][0]", 1),
//      ("/system['sys'][0]/system['subsystem 1'][0]/node['b'][1]", 1),
//      ("/system['sys'][0]/system['subsystem 1'][0]/node['b'][2]", 1),
//      ("/system['sys'][0]/system['subsystem 1'][0]/node['b'][3]", 0)
    )

    tests.foreach { tuple=>
      val results = program.find(tuple._1)
      assert( results.size==tuple._2)
    }
    /*
system  "sys" {

    node "a" {
        props {
            count: 1
        }
    }


    system "subsystem 1" {
        node "b" {
            props {
                count: 3
            }
        }
    }
}
     */
  }
}
