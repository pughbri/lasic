package com.lasic

//import interpreter.actors.{NodeActor}
//import interpreter.{Deploy, DeployActor}
import cloud.AmazonCloud
import cloud.mock.MockCloud
import interpreter.DeployVerb
import java.io.File
import parser.LasicCompiler
import io.Source
import java.lang.System
//object Foo {
//  def unapplySeq(args:Array[String]): Option[Seq[String]] = {
//    Some(args)
//  }
//}

/**
 *
 *
 */
object Lasic {
  var verbose = false

  var cloudProvider = CloudProvider.Amazon
  var lasicFile: String = null
  var verb: String = null

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
    println("Usage: java -jar lasic.jar [options] <verb> <lasic-program>")
    System.exit(1)
  }

  def parseArgs(args: Array[String]): Unit = {
    for (arg <- args) arg match {
      case "-h" | "--help" => printUsageAndExit("Lasic Help:")
      case "-v" | "--verbose" => verbose = true
      case ArgOption("c" | "cloud", provider) => cloudProvider = CloudProvider.withName(provider)
      case ArgOption(_, _) => printUsageAndExit("invalid option:" + arg)
      case cmd => {
        if (verb == null) {
          verb = cmd
        }
        else if (lasicFile == null) {
          lasicFile = cmd
        }
        else printUsageAndExit("Too many commands:" )
      }    
    }
    if (verb == null || lasicFile == null) {
      printUsageAndExit("must provide both a verb and lasic-program:")
    }
  }

  def runLasic(args: Array[String]): Unit = {
    parseArgs(args)


    val s = Source.fromFile(new File(lasicFile))
    val program = LasicCompiler.compile(s)

    val cloud = cloudProvider match {
      case CloudProvider.Amazon => new AmazonCloud()
      case CloudProvider.Mock => new MockCloud(1)
      case _ => new MockCloud(1)
    }

    val deploy = new DeployVerb(cloud, program)
    deploy.doit

  }

  def main(args: Array[String]) {
    val startTime = System.currentTimeMillis
    runLasic(args)
    println("Ran " + lasicFile + " in " + (System.currentTimeMillis - startTime / 1000) + " seconds.")
    System.exit(0)


    //    val deploy = new DeployActor(cloud)
    //    deploy.deploy(program)



    //    bar(foo)
    //    println(foo);

    //    val program = LasicCompiler.compile(argv(0))
    //    val interpreter = new DeployActor

    3
  }
}
