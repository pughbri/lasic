package com.lasic

//import interpreter.actors.{NodeActor}
//import interpreter.{Deploy, DeployActor}
import interpreter.DeployVerb
import java.io.File
import parser.LasicCompiler
import io.Source
import com.lasic.cloud.mock.MockCloud


//object Foo {
//  def unapplySeq(args:Array[String]): Option[Seq[String]] = {
//    Some(args)
//  }
//}

/**
 * Hello world!
 *
 */
object Lasic {
  def mk(more: Int) = (x: Int) => x + more

  def main(argv: Array[String]) {
    val s = Source.fromFile( new File(argv(0)))
    val program = LasicCompiler.compile(s)

    val cloud = new MockCloud(1)
    val deploy = new DeployVerb(cloud,program)
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
