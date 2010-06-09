package com.lasic.model

/**
 * Created by IntelliJ IDEA.
 * User: lmonson
 * Date: May 13, 2010
 * Time: 9:00:50 PM
 * To change this template use File | Settings | File Templates.
 */

trait Pathable {
  def path: String
  def parent: Pathable
  def children: List[Pathable]

  def root:LasicProgram = {
    if (parent == null) this.asInstanceOf[LasicProgram]
    else parent.root
  }

  def find(path: String) = {
    everything.filter{ thing => thing.matches(path)}
//    path match {
//      case "//node[*][*]" => allNodeInstances(root)
//      case _ => List()
//    }
  }

  def matches(path:String) = {
    (path,this) match {
      case ("//node[*][*]",thing:NodeInstance) => true
      case ("//node[*]", thing:NodeGroup) => true
      case ("//system[*][*]", thing:SystemInstance) => true
      case ("//system[*]", thing:SystemGroup) => true
      case (path,thing) => path==thing.path
    }
  }
  

//  private def allNodeInstances(p:Pathable):List[Pathable] = {
//    var result = p.children.filter { child => child.isInstanceOf[NodeInstance] }
//    var x = p.children.map{ allNodeInstances }
//    var y:List[Pathable] = x.flatten
//    result ::: y
//  }

  private def everything:List[Pathable] = {
    val thisThing = List(this)
    var thoseThings = children.map{ child => child.everything }
    val thoseThingsList:List[Pathable] = thoseThings.flatten
    thisThing ::: thoseThingsList
  }



}