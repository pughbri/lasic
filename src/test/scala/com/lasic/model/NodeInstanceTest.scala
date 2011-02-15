package com.lasic.model

import org.scalatest.FlatSpec
import org.scalatest.matchers.ShouldMatchers
import com.lasic.values.{ResolvedScriptArgument, ResolvedScriptDefinition, ScriptDefinition}
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

/**
 *
 * @author Brian Pugh
 */

@RunWith(classOf[JUnitRunner])
class NodeInstanceTest extends FlatSpec with ShouldMatchers with ProgramLoader {

  "NodeInstance" should "add variables NAME and INDEX to script params" in {
    val program = getLasicProgram(1)

    //grab the node
    val nodes = program.findNodes("/system['www.lasic.com'][0]/node['www-lasic-load-balancer'][0]")
    nodes should have size (1)
    val node = nodes(0)

    //grab the scripts for the node and resolve them
    node.parent.actions should have size (1)
    val allScripts = List[ScriptDefinition]() ++ node.parent.actions(0).scriptDefinitions
    val resolvedScripts = node.resolveScripts(allScripts)

    //validate that everything resolved properly to the nodes private DNS
    resolvedScripts should have size (1)
    resolvedScripts(0).scriptName should be === "~/install-lasic-lb.sh"
    println(resolvedScripts(0).scriptArguments.size)
    resolvedScripts(0).scriptArguments should have size (4)
    val argOption: Option[ResolvedScriptArgument] = resolvedScripts(0).scriptArguments find (_.argName == "NAME")
    val arg: ResolvedScriptArgument = argOption match {
      case None => fail("Argument value NAME not found for script " + resolvedScripts(0).scriptName)
      case Some(x) => x
    }

    arg.argValues should have size (1)
    arg.argValues(0).literal should be === "www-lasic-load-balancer"

    val argOption2: Option[ResolvedScriptArgument] = resolvedScripts(0).scriptArguments find (_.argName == "INDEX")
    val arg2: ResolvedScriptArgument = argOption2 match {
      case None => fail("Argument value INDEX not found for script " + resolvedScripts(0).scriptName)
      case Some(x) => x
    }
    arg2.argValues should have size (1)
    arg2.argValues(0).literal should be === "0"
  }
}