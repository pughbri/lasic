package com.lasic.model

import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith
import org.scalatest.FlatSpec
import org.scalatest.matchers.ShouldMatchers
import org.apache.commons.io.IOUtils
import com.lasic.parser.LasicCompiler
import com.lasic.cloud.mock.{MockCloud, MockVM}

/**
 *
 * @author Brian Pugh
 */

@RunWith(classOf[JUnitRunner])
class ScriptResolverTest extends FlatSpec with ShouldMatchers {
  def getLasicProgram(i: Int) = {
    val path = "/model/Program%03d.lasic".format(i)
    val is = getClass.getResourceAsStream(path)
    val program = IOUtils.toString(is);
    LasicCompiler.compile(program)
  }


  "ScriptResolver" should "resolve a node path to the instance's private DNS" in {

    val program = getLasicProgram(1)

    //set the private DNS on the node that needs to be resolved
    val nodes = program.findNodes("/system['www.lasic.com'][0]/node['www-lasic-load-balancer'][0]")
    nodes should have size (1)
    val mockVm = new MockVM(new MockCloud)
    mockVm.privateDNS = "private-dns-test"
    nodes(0).vm =  mockVm


    //grab the scale group
    val scaleGroups = program.find("/system['www.lasic.com'][0]/scale-group['www-lasic-webapp']") match {
      case x: List[ScaleGroupInstance] => x
      case _ => {fail("expected a list of ScaleGroupInstance"); null}
    }
    scaleGroups should have size (1)
    val scaleGroup = scaleGroups(0)


    //grab the scripts for the scale group and resolve them
    scaleGroup.actions should have size (1)
    val allScripts = Map[String, Map[String, ArgumentValue]]() ++ scaleGroup.actions(0).scriptMap
    val resolvedScripts: Map[String, Map[String, List[String]]] = ScriptResolver.resolveScripts(scaleGroups(0), allScripts)

    //validate that everything resolved properly to the nodes private DNS
    resolvedScripts should have size (1)
    val variableValueMap = resolvedScripts("~/install-lasic-webapp.sh")
    val values = variableValueMap("NODE")
    values should have size (1)
    values(0) should be === "private-dns-test"
  }

  "ScriptResolver" should "resolve a scalegroup path to the scalegroup cloud name" in {

    val program = getLasicProgram(1)

    //grab the node
    val nodes = program.findNodes("/system['www.lasic.com'][0]/node['www-lasic-load-balancer'][0]")
    nodes should have size (1)
    val node = nodes(0)


    //grab the scripts for the node and resolve them
    node.parent.actions should have size (1)
    val allScripts = Map[String, Map[String, ArgumentValue]]() ++ node.parent.actions(0).scriptMap
    val resolvedScripts: Map[String, Map[String, List[String]]] = ScriptResolver.resolveScripts(nodes(0), allScripts)

    //validate that everything resolved properly to the nodes private DNS
    resolvedScripts should have size (1)
    val variableValueMap = resolvedScripts("~/install-lasic-lb.sh")
    val values = variableValueMap("SCALEGROUP")
    values should have size (1)
    values(0) should be === "www-lasic-webapp-01"
  }
}