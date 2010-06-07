package com.lasic

//import interpreter.actors.{NodeActor}
//import interpreter.{Deploy, DeployActor}
import cloud.AmazonCloud
import cloud.mock.MockCloud
import interpreter.DeployVerb
import java.io.File
import parser.LasicCompiler
import io.Source
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
  var lasicFile = ""

  object CloudProvider extends Enumeration {
    type CloudProviders = Value
    val Amazon = Value("aws")
    val Mock = Value("mock")
  }

  object OptionWithValue {
    def unapply(str: String): Option[(String, String)] = {
      val parts = str split "="
      if (parts.length == 2) Some(parts(0),parts(1)) else None
    }
  }


  def printUsageAndExit() = {
    println("Usage: java -jar lasic.jar <verb> <lasic-program>")
    System.exit(1)
  }

  def parseArgs(args: Array[String]): Unit = {
    for (arg <- args) arg match {
      case "-h" | "--help" => printUsageAndExit
      case "-v" | "--verbose" => verbose = true
      case OptionWithValue("-c" | "--cloud", provider) => cloudProvider = CloudProvider.withName(provider)
      case file =>    lasicFile = file
      //todo: need to parse out filename and verb then handle unknown args
//      case x =>
//        println("Unknown option: '" + x + "'")
//        printUsageAndExit
    }
  }

  def main(args: Array[String]) {
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
