package com.lasic

import cloud.amazon.AmazonCloud
import cloud.mock.MockCloud
import interpreter.{RunActionVerb, DeployVerb}
import java.io.File
import model.LasicProgram
import parser.LasicCompiler
import io.Source
import java.lang.System


/**
 *
 *
 */
object Lasic {
  var verbose = false

  var cloudProvider = CloudProvider.Amazon
  var lasicFile: String = null
  var verbArg: String = null
  var actionName: String = null

  object CloudProvider extends Enumeration {
    type CloudProviders = Value
    val Amazon = Value("aws")
    val Mock = Value("mock")
  }

  object ArgOption {
    def unapply(str: String): Option[(String, String)] = {
      if (!(str startsWith "-")) {
        None
      }
      else {
        val optionWithDashStripped = {
          if (str startsWith "--") {
            str substring 2
          }
          else {
            str substring 1
          }
        }
        val parts = optionWithDashStripped split "="
        parts.length match {
          case 1 => Some(parts(0), null)
          case 2 => Some(parts(0), parts(1))
          case _ => None
        }
      }
    }
  }


  def printUsageAndExit(message: String) = {
    println(message)
    println(
      """Usage: java -jar lasic.jar [options] <verb> <lasic-program>
   supported verbs: deploy, runAction""")
    System.exit(1)
  }

  def parseArgs(args: Array[String]): Unit = {
    for (arg <- args) arg match {
      case "-h" | "--help" => printUsageAndExit("Lasic Help:")
      case "-v" | "--verbose" => verbose = true
      case ArgOption("c" | "cloud", provider) => cloudProvider = CloudProvider.withName(provider)
      case ArgOption("a" | "action", actionNameArg) => actionName = actionNameArg
      case ArgOption(_, _) => printUsageAndExit("invalid option:" + arg)
      case cmd => {
        if (verbArg == null) {
          verbArg = cmd
        }
        else if (lasicFile == null) {
          lasicFile = cmd
        }
        else printUsageAndExit("Too many commands:")
      }
    }
    if (verbArg == null || lasicFile == null) {
      printUsageAndExit("must provide both a verb and lasic-program:")
    }
  }

  def compile: LasicProgram = {
    val s = Source.fromFile(new File(lasicFile))
    LasicCompiler.compile(s)
  }

  def execute(program: LasicProgram): Any = {
    val cloud = cloudProvider match {
      case CloudProvider.Amazon => new AmazonCloud()
      case CloudProvider.Mock => new MockCloud(1)
      case _ => new MockCloud(1)
    }

    val verb = verbArg match {
      case "deploy" => new DeployVerb(cloud, program)
      case "runAction" => new RunActionVerb(actionName, cloud, program)
      case _ => printUsageAndExit("unknown verb: " + verbArg); null
    }
    verb.doit
  }

  def runLasic(args: Array[String]): Unit = {
    parseArgs(args)
    val program = compile
    execute(program)
  }

  def main(args: Array[String]) {
    val startTime = System.currentTimeMillis
    runLasic(args)
    println("Ran " + lasicFile + " in " + ((System.currentTimeMillis - startTime) / 1000) + " seconds.")
    System.exit(0)


    3
  }
}
