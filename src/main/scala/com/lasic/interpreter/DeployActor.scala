package com.lasic.interpreter

import actors._
import com.lasic.model.{NodeInstance, LasicProgram}
//import ParallelSimulation._
import collection.mutable.ListBuffer
import scala.actors.Actor._
import com.lasic.{Cloud, VM}
import scala.actors.Futures._
import com.lasic.cloud.LaunchConfiguration
import scala.actors.{Future, Actor}


case class Deploy(program: LasicProgram)


class DeployActor(cloud: Cloud) /* extends Clock */ {

  private def setupNode(node: NodeInstance) : Future[Any] = {
    val f = future {
      val vm = cloud.createVMs(new LaunchConfiguration(node), 1, true )(0);
    }
    f
//
//    val nodeActor = new NodeActor(node, this, cloud)
//    add(nodeActor)
//
//    val work = WorkItem(1, NodeLaunch(), nodeActor)
//    insert(work)
  }

  private def waitForLaunch(vm:VM) = {
  }

  def deploy(prog: LasicProgram)  {
    val lb = new ListBuffer()

    val futureList = prog.find("//node[*][*]").map {x => setupNode(x.asInstanceOf[NodeInstance])}
    val results = awaitAll(1000, futureList:_*)
    for(i <- results) {
      i match {
        case Some(x) => println(x)
        case None => println("none")
      }
//      val x = i.asInstanceOf[Future[VM]]
//       println(x.isSet)
    }
//
//
//    //      node =>
//    //              setupNode(node.asInstanceOf[NodeInstance])
//    //    }
//
//    this ! Start
  }

  //  def deployed(vm: VM, handle:Any) = {
  //    val node = handle.asInstanceOf[NodeInstance]
  //    println("Deployed: " + vm.instanceId)
  //  }
  //
  //  def act() {
  //    Actor.loop {
  //      react {
  //        case Deploy(prog) => deploy(prog)
  //        case CloudNodeLaunched(vm, handle) => deployed(vm,handle)
  //        case msg => println("Unknown msg sent to deploy actor: " + msg)
  //      }
  //    }
  //  }
}

//import scala.actors.Futures._
//import scala.actors.Future
////
//
//
//case class A
//case class B
//case class C
//case class D
//
//
//object Foo {
//  def main(args: Array[String]) {
//    val actorA = actor {
//      loop {
//        react {
//          case A => println("A")
//          case B => {
//            println("B")
//            self !! D
//            react {
//              case D => println("inner D")
//            }
//          }
//          case C => println("C")
//          //case D=> println("outer D")
//        }
//      }
//    }
//
//    actorA !! A
//    actorA !! B
//    actorA !! C
//    actorA !! A
//
//
//
//
//
//
//    //     println("main: "+Thread.currentThread().getName);
//    //
//    //
//    ////
//    ////
//    //     var futureList:List[Future[Int]] = List()
//    //     for(i <- 1 to 1000)
//    //       futureList = futureList ::: List(future[Int]({
//    //        println(Thread.currentThread.getName);
//    //         //Thread.sleep(500)
//    //
//    //        i+2}))
//    //
//    //     Thread.sleep(5000)
//    3
//  }
//}
