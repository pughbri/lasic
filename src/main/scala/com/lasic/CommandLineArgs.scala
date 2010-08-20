package com.lasic

import com.beust.jcommander.Parameter
/**
 *
 * @author Brian Pugh
 */

class CommandLineArgs {

  @Parameter(names = Array("-h", "--help"), description = "Print usage")
  var help = false

  @Parameter(names = Array("-c",  "--cloud"), description = "Determines which cloud provider will be used.  Options are aws and mock")
  var cloud = "aws"


  @Parameter(names = Array("-a", "--action"), description = "Action to be performed when used with the runAction verb")
  var action:String = null

  @Parameter(description = "verb script")
  var verbAndScript:java.util.List[String] = null
}