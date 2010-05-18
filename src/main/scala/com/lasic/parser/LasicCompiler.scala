package com.lasic.parser

import ast.{ASTNode, ASTSystem}
import collection.mutable.ListBuffer
import scala.util.matching.Regex
import org.apache.commons.io.IOUtils
import com.lasic.model._

/**
 * Created by IntelliJ IDEA.
 * User: lmonson
 * Date: May 12, 2010
 * Time: 8:29:34 PM
 * To change this template use File | Settings | File Templates.
 */

object LasicCompiler {
  var blockComment = """(/\*([^*]|(\*+[^*/]))*\*+/)|(//.*)""".r

  private def stripComments(s:String) = {
    blockComment.replaceAllIn(s, " ")
  }
  
  private def createInstances(nodeGroup: NodeGroup) = {
    // Create all the instances
    val lb = new ListBuffer[NodeInstance]()
    for (i <- 0 until nodeGroup.count) {
      val nodeInstance = new NodeInstance(nodeGroup, i)
      lb += nodeInstance
    }
    nodeGroup.instances = lb.toList

  }

  private def createInstances(sysGroup: SystemGroup) = {
    val lb = new ListBuffer[SystemInstance]()
    for (i <- 0 until sysGroup.count) {
      val sysInstance = new SystemInstance(sysGroup, i)
      lb += sysInstance
    }
    sysGroup.instances = lb.toList

  }

  private def createNodeGroups(ast: ASTSystem, sysGroup: SystemGroup) = {
    // create nodes
    sysGroup.instances.foreach {
      systemInstance =>
        systemInstance.nodegroups = ast.nodes.toList.map {
          case node =>
            val nodeGroup:NodeGroup = compile(node);
            nodeGroup.parentSystemInstance = systemInstance
            nodeGroup
        }
    }
  }

  private def createSubsystems(ast: ASTSystem, sysGroup: SystemGroup) = {
    sysGroup.instances.foreach {
      instance:SystemInstance =>
        instance.subsystems = ast.subsystems.toList.map {
          subsys:ASTSystem => compile(subsys, instance)
        }
    }
  }

  def compile(program: String): LasicProgram = {
    val reducedProgram = stripComments(program)
    val p = new LasicParser()
    p.parseAll(p.system, reducedProgram) match {
      case p.Success(r: ASTSystem, _) => compile(r)
      case x => throw new RuntimeException("compilation failure: " + x);
    }
  }

  private def compile(ast:ASTSystem):LasicProgram = {
    val lp = new LasicProgram
    val rootSystem = compile(ast,lp)
    lp.rootGroup = rootSystem
    lp
  }

  private def compile(ast: ASTSystem, parent:Pathable): SystemGroup = {
    // Create the group
    val sysGroup = new SystemGroup(parent)
    sysGroup.name = ast.name
    sysGroup.count = ast.count


    // Create all the instances
    createInstances(sysGroup)

    // Create all the nodegroups in each SystemInstance
    createNodeGroups(ast, sysGroup)

    // Create all the subsystems
    createSubsystems(ast, sysGroup)

    // for each instance, create the nodegroups
    //    sysGroup.instances.foreach {
    //      instance =>
    //        for()
    //    }
    sysGroup

  }

  private def compile(ast: ASTNode): NodeGroup = {
    // Create the group
    val nodeGroup = new NodeGroup
    nodeGroup.name = ast.name
    nodeGroup.count = ast.count
    nodeGroup.machineimage = ast.machineimage
    nodeGroup.kernelid = ast.kernelid
    nodeGroup.ramdiskid = ast.ramdiskid
    nodeGroup.groups = ast.groups.map { x=>x }
    nodeGroup.key = ast.key
    nodeGroup.user = ast.user
    nodeGroup.instancetype = ast.instancetype
    nodeGroup.scpMap = ast.scpMap
    nodeGroup.scriptMap = ast.scriptMap


    createInstances(nodeGroup)
    nodeGroup


  }
}