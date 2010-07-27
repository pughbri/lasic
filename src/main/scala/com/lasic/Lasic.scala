package com.lasic

import cloud.amazon.AmazonCloud
import cloud.mock.MockCloud
import interpreter.{RunActionVerb, DeployVerb, DeployVerb2}
import model.LasicProgram
import parser.LasicCompiler
import io.Source
import java.lang.System
import com.beust.jcommander.{ParameterException, JCommander}
import java.io.{FileNotFoundException, File}


/**
 *
 *
 */
object Lasic {

  object CloudProvider extends Enumeration {
    type CloudProviders = Value
    val Amazon = Value("aws")
    val Mock = Value("mock")
  }

  def main(args: Array[String]) {
    val startTime = System.currentTimeMillis
    runLasic(args)
    println("Ran in " + ((System.currentTimeMillis - startTime) / 1000) + " seconds.")
    System.exit(0)

    3
  }

  def runLasic(args: Array[String]): Unit = {
    val cmdLineArgs = parseArgs(args)
    val program = compile(cmdLineArgs)
    execute(program, cmdLineArgs)
  }

  def parseArgs(args: Array[String]): CommandLineArgs = {
    val cmdLineArgs = new CommandLineArgs
    var jCommander: JCommander = null
    try {
      jCommander = JCommanderFactory.createWithArgs(cmdLineArgs)
      jCommander.parse(args: _*)
    }
    catch {
      case e: ParameterException => printUsageAndExit(jCommander, e.getMessage)
    }

    if (cmdLineArgs.help) {
      printUsageAndExit(jCommander, "Printing help...")
    }
    if (cmdLineArgs.verbAndScript == null || cmdLineArgs.verbAndScript.size != 2) {
      printUsageAndExit(jCommander, "Must provide both a verb and script")
    }
    cmdLineArgs
  }

  def compile(cmdLineArgs: CommandLineArgs): LasicProgram = {
    try {
      val s = Source.fromFile(new File(cmdLineArgs.verbAndScript.get(1)))
      LasicCompiler.compile(s)
    }
    catch {
      case e: FileNotFoundException => printUsageAndExit(null, "Unable to find file: [" + cmdLineArgs.verbAndScript.get(1) + "]"); null
    }
  }

  def execute(program: LasicProgram, cmdLineArgs: CommandLineArgs): Any = {
    val cloudProvider = CloudProvider.withName(cmdLineArgs.cloud) match {
      case CloudProvider.Amazon => new AmazonCloud()
      case CloudProvider.Mock => new MockCloud(1)
      case _ => new MockCloud(1)
    }

    val verb = cmdLineArgs.verbAndScript.get(0) match {
      case "deploy" => new DeployVerb(cloudProvider, program)
      case "deploy2" => new DeployVerb2(cloudProvider, program)
      case "runAction" => new RunActionVerb(cmdLineArgs.action, cloudProvider, program)
      case _ => printUsageAndExit(null, "unknown verb: " + cmdLineArgs.verbAndScript.get(0)); null
    }

    verb.doit
  }

  def printUsageAndExit(jcmder: JCommander, message: String) = {
    println(message)
    if (jcmder != null) {
      jcmder.usage
    }
    System.exit(1)
  }

}
