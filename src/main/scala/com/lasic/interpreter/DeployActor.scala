package com.lasic.interpreter

import actors._
import com.lasic.model.{NodeInstance, LasicProgram}
import com.lasic.{Cloud, VM}
import se.scalablesolutions.akka.actor.Actor._
import com.lasic.cloud.LaunchConfiguration
import se.scalablesolutions.akka.actor.{ActorRef, Actor}


//class NodeActor(cloud:Cloud, node:NodeInstance) extends Actor {
//  def receive = {
//    case Launch => {
//      val vm = cloud.createVM(new LaunchConfiguration(node), true)
////      while( !vm.isInitialized) {
////        Thread.sleep(1000)
////      }
////      reply(Some(vm))
//      3
//    }
//
//  }
//}


object DeployState extends Enumeration {
  type DeployState = Value
  val Start, CreatingVMs, Finished = Value
}

object DeployCommand extends Enumeration {
  type DeployCommand = Value
  val Go, DeployStateQ = Value
}



class NodeTracker(val actor:ActorRef, val node:NodeInstance) {
  var _instanceID:String = null
  def instanceID:String = {
    if ( _instanceID==null ) {
      val x:Option[Nothing] = actor !! QueryID
      val y:String = x.get
      if ( y!=null )
        _instanceID = y.toString
    }
    _instanceID
  }
}


class DeployVerb(cloud: Cloud, program: LasicProgram) {
  //  var deployState = Start
  //
  //  def receive = {
  //    case (DeployStateQ, a:Actor) => a ! deployState
  //    case Go => {
  //      deployState match {
  //        case Start => deploy()
  //        case _ => // no state change
  //      }
  //    }
  //    case x => println("What are you talking about? "+x)
  //  }

  def notBootedList(nodes:List[NodeTracker])= {
    nodes.filter {
      tuple =>
        tuple.actor !! QueryNodeState match {
          case Some(VMActorState.Booted) =>  false
          case x => true
        }
    }
  }

  def vmIDs(nodes:List[NodeTracker])= {
    nodes.map {
      traker =>
        val s:String = traker.instanceID
        if ( s!=null )
          s
        else
          "(id not assigned)"

    }
  }

  def deploy() {
    val nodes1 = program.find("//node[*][*]")
    val nodes = nodes1.map {
      _node =>
        val node = _node.asInstanceOf[NodeInstance]
        val actor = Actor.actorOf(new VMActor(cloud)).start
        actor ! new Launch(new LaunchConfiguration(node))
        new NodeTracker(actor,node)
    }
    var waiting = notBootedList(nodes)
    while( !waiting.isEmpty ) {
      val descriptions:List[String] = vmIDs(waiting);
      println("Waiting for machines to boot: " + descriptions)
      Thread.sleep(500)
      waiting = notBootedList(nodes)
    }
    println("All booted!!!")
    println(vmIDs(nodes))

    //    val futureList = prog.find("//node[*][*]").map {x => setupNode(x.asInstanceOf[NodeInstance])}


  }

}
//case class Deploy(program: LasicProgram)
//
//
//
//
//class DeployActor(cloud: Cloud) /* extends Clock */ {
//
//  private def setupNode(node: NodeInstance) : Future[Any] = {
//    val f = future {
//      val vm = cloud.createVMs(new LaunchConfiguration(node), 1, true )(0);
//    }
//    f
////
////    val nodeActor = new NodeActor(node, this, cloud)
////    add(nodeActor)
////
////    val work = WorkItem(1, NodeLaunch(), nodeActor)
////    insert(work)
//  }
//
//  private def waitForLaunch(vm:VM) = {
//  }
//
//  def deploy(prog: LasicProgram)  {
//    val lb = new ListBuffer()
//
//    val futureList = prog.find("//node[*][*]").map {x => setupNode(x.asInstanceOf[NodeInstance])}
//    val results = awaitAll(1000, futureList:_*)
//    for(i <- results) {
//      i match {
//        case Some(x) => println(x)
//        case None => println("none")
//      }
////      val x = i.asInstanceOf[Future[VM]]
////       println(x.isSet)
//    }
////
////
////    //      node =>
////    //              setupNode(node.asInstanceOf[NodeInstance])
////    //    }
////
////    this ! Start
//  }
//
//  //  def deployed(vm: VM, handle:Any) = {
//  //    val node = handle.asInstanceOf[NodeInstance]
//  //    println("Deployed: " + vm.instanceId)
//  //  }
//  //
//  //  def act() {
//  //    Actor.loop {
//  //      react {
//  //        case Deploy(prog) => deploy(prog)
//  //        case CloudNodeLaunched(vm, handle) => deployed(vm,handle)
//  //        case msg => println("Unknown msg sent to deploy actor: " + msg)
//  //      }
//  //    }
//  //  }
//}
//
////import scala.actors.Futures._
////import scala.actors.Future
//////
////
////
////case class A
////case class B
////case class C
////case class D
////
////
////object Foo {
////  def main(args: Array[String]) {
////    val actorA = actor {
////      loop {
////        react {
////          case A => println("A")
////          case B => {
////            println("B")
////            self !! D
////            react {
////              case D => println("inner D")
////            }
////          }
////          case C => println("C")
////          //case D=> println("outer D")
////        }
////      }
////    }
////
////    actorA !! A
////    actorA !! B
////    actorA !! C
////    actorA !! A
////
////
////
////
////
////
////    //     println("main: "+Thread.currentThread().getName);
////    //
////    //
////    ////
////    ////
////    //     var futureList:List[Future[Int]] = List()
////    //     for(i <- 1 to 1000)
////    //       futureList = futureList ::: List(future[Int]({
////    //        println(Thread.currentThread.getName);
////    //         //Thread.sleep(500)
////    //
////    //        i+2}))
////    //
////    //     Thread.sleep(5000)
////    3
////  }
////}
