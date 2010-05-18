package com.lasic

import com.lasic.parser.LasicCompiler
import junit.framework.TestCase
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

  def testQueryForNodes = {
    val program = getLasicProgram(1)

    val tests = List(
      ("//node[*][*]",4),
      ("//node[*]",2),
      ("//system[*][*]",2),
      ("//system[*]",2),
      ("/system['sys'][0]", 1),
      ("/system['sys']", 1),
      ("/system['sys'][0]/system['subsystem 1'][0]", 1),
      ("/system['sys'][0]/system['subsystem 1']", 1),
      ("/system['sys'][0]/node['a']", 1),
      ("/system['sys'][0]/node['a'][0]", 1),
      ("/system['sys'][0]/system['subsystem 1'][0]/node['b']", 1),
      ("/system['sys'][0]/system['subsystem 1'][0]/node['b'][0]", 1),
      ("/system['sys'][0]/system['subsystem 1'][0]/node['b'][1]", 1),
      ("/system['sys'][0]/system['subsystem 1'][0]/node['b'][2]", 1),
      ("/system['sys'][0]/system['subsystem 1'][0]/node['b'][3]", 0),
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
