package com.lasic

import cloud.MockCloud
import interpreter.actors.{NodeActor, CloudActor}
import interpreter.{Deploy, DeployActor}
import java.io.File
import parser.LasicCompiler
import io.Source


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
    println(s.toString())
    val program = LasicCompiler.compile(s)


    CloudActor.start( new MockCloud(1) )
    NodeActor.start()
    DeployActor.start()

    DeployActor ! Deploy(program)

    var foo = 3
    val a = mk(foo)
    foo = 100
    println(a(1))



    //    bar(foo)
    //    println(foo);

    //    val program = LasicCompiler.compile(argv(0))
    //    val interpreter = new DeployActor


  }
}
