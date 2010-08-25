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

  def findFirst(path:String):Pathable = {
    find(path).first
  }
  
  def find(path: String):List[Pathable] = {
    if ( path.startsWith("/") && this!=root )
      root.find(path)
    else {
      val x = everything
      val y = x.filter{
        thing =>
          thing.matches(path)
      }
      y

    }

//    path match {
//      case "//node[*][*]" => allNodeInstances(root)
//      case _ => List()
//    }
  }

  def findNodes(path:String):List[NodeInstance] = {
    val a = find(path)
    val b = a.filter{ z=> z.isInstanceOf[NodeInstance] }
    val c= b.map{ _.asInstanceOf[NodeInstance]}
    c
  }

  def matches(path:String) = {
    (path,this) match {
      case ("//node[*][*]",thing:NodeInstance) => true
      case ("//node[*]", thing:NodeGroup) => true
      case ("//system[*][*]", thing:SystemInstance) => true
      case ("//system[*]", thing:SystemGroup) => true
      case ("//scale-group[*]", thing:ScaleGroupInstance) => true
      case ("//scale-group-configuration[*]", thing:ScaleGroupConfiguration) => true
      case (path,thing) => {
        val mypath = thing.path
        path.equals(mypath)
      }
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

  override def toString = this.getClass().getSimpleName() + ": " + path



}