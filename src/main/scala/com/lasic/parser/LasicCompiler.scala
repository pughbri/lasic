package com.lasic.parser

import ast._
import collection.mutable.ListBuffer
import com.lasic.model._
import io.Source
import java.lang.String
import com.lasic.values.{NodeProperties, BaseAction}

/**
 * Created by IntelliJ IDEA.
 * User: lmonson
 * Date: May 12, 2010
 * Time: 8:29:34 PM
 * To change this template use File | Settings | File Templates.
 */

object LasicCompiler {
  var blockComment = """(/\*([^*]|(\*+[^*/]))*\*+/)|(//.*)""".r

  private def stripComments(s: String) = {
    blockComment.replaceAllIn(s, " ")
  }

  private def createVolumeInstances(nodeInstance: NodeInstance, volumeProperties: Map[String, String]) = {
    val device = {
      if (volumeProperties.contains("device")) volumeProperties("device") else null
    }
    val mount = {
      if (volumeProperties.contains("mount")) volumeProperties("mount") else null
    }

    val size = {
      val sizeStr = volumeProperties("size")
      if (sizeStr.matches("\\d*"))
        sizeStr.toInt
      else {
        if (sizeStr.matches("\\d*g"))
          sizeStr.split('g')(0).toInt
        else
          throw new IllegalArgumentException("invalid volume size.  Size must be all digits followed by an optional 'g'. The Unit is gigabytes " + sizeStr)
      }
    }

    val volumeInstance = new VolumeInstance(nodeInstance,
      volumeProperties("name"),
      size,
      device,
      mount)

    nodeInstance.volumes = volumeInstance :: nodeInstance.volumes
  }

  private def createInstances(nodeGroup: NodeGroup, ast: ASTNode) = {
    // Create all the instances
    val lb = new ListBuffer[NodeInstance]()
    for (i <- 0 until nodeGroup.count) {
      val nodeInstance = new NodeInstance(nodeGroup, i)
      ast.volumes.foreach(volume => createVolumeInstances(nodeInstance, Map.empty ++ volume))
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
            val nodeGroup: NodeGroup = compile(node);
            nodeGroup.parentSystemInstance = systemInstance
            nodeGroup
        }
    }
  }

  private def createScaleGroups(ast: ASTSystem, sysGroup: SystemGroup) = {
    // create nodes
    sysGroup.instances.foreach {
      systemInstance =>
        systemInstance.scaleGroups = ast.scaleGroups.toList.map {
          case astScaleGroup =>
            val scaleGroup = compile(astScaleGroup);
            scaleGroup.parentSystemInstance = systemInstance
            scaleGroup
        }
    }
  }

  private def createSubsystems(ast: ASTSystem, sysGroup: SystemGroup) = {
    sysGroup.instances.foreach {
      instance: SystemInstance =>
        instance.subsystems = ast.subsystems.toList.map {
          subsys: ASTSystem => compile(subsys, instance)
        }
    }
  }

  def compile(program: Source): LasicProgram = {
    val b = new StringBuilder
    program.addString(b, "", "", "")
    compile(b.toString)
  }

  def compile(program: String): LasicProgram = {
    val reducedProgram = stripComments(program)
    val p = new LasicParser()
    p.parseAll(p.system, reducedProgram) match {
      case p.Success(r: ASTSystem, _) => compile(r)
      case x => throw new RuntimeException("compilation failure: " + x);
    }
  }

  private def compile(ast: ASTSystem): LasicProgram = {
    val lp = new LasicProgram
    val rootSystem = compile(ast, lp)
    lp.rootGroup = rootSystem
    bindPaths(rootSystem, ast.boundPaths)
    lp
  }

  private def compile(ast: ASTSystem, parent: Pathable): SystemGroup = {
    // Create the group
    val sysGroup = new SystemGroup(parent)
    sysGroup.name = ast.name
    sysGroup.count = ast.count



    // Create all the instances
    createInstances(sysGroup)

    // Create all the nodegroups in each SystemInstance
    createNodeGroups(ast, sysGroup)

    // Create all the scale groups in each SystemInstance
    createScaleGroups(ast, sysGroup)

    // Create all the subsystems
    createSubsystems(ast, sysGroup)

    // for each instance, create the nodegroups
    //    sysGroup.instances.foreach {
    //      instance =>
    //        for()
    //    }

    sysGroup
  }

  private def bindPaths(sysGroup: SystemGroup, boundPaths: Map[String, String]) {
    boundPaths.foreach {
      (boundPath) => {
        val pathables = sysGroup.find(boundPath._1)
        if (pathables.size != 1) {
          throw new Exception("path " + boundPath + " does not map to exactly one pathable.  Path List: " + pathables.mkString(", "))
        }
        else {
          pathables(0) match {
            case nodeInstance: NodeInstance => nodeInstance.boundInstanceId = boundPath._2
            case _ => throw new Exception("Path " + boundPath + " does not resolve to a nodeinstance.  Resolves to " + pathables(0).getClass)
          }
        }

      }
    }
  }

  def copyNodeProperties(to: NodeProperties, from: NodeProperties): Unit = {
    to.name = from.name
    to.count = from.count
    to.machineimage = from.machineimage
    to.kernelid = from.kernelid
    to.ramdiskid = from.ramdiskid
    to.groups = from.groups.map {x => x}
    to.key = from.key
    to.user = from.user
    to.instancetype = from.instancetype
  }

  private def compile(ast: ASTNode): NodeGroup = {
    // Create the group
    val nodeGroup = new NodeGroup
    copyNodeProperties(nodeGroup, ast)
    nodeGroup.actions = compile(ast.actions)
    createInstances(nodeGroup, ast)
    nodeGroup
  }

  private def compile(actions: List[BaseAction]): List[Action] = {
    actions map {
      case astAction: ASTAction => {
        val action = new Action()
        action.name = astAction.name
        action.scpMap = astAction.scpMap
        action.scriptMap = astAction.scriptMap
        action
      }
    }

  }

  private def compile(ast: ASTScaleGroup): ScaleGroupInstance = {
    val scaleGrpConfig = new ScaleGroupConfiguration
    copyNodeProperties(scaleGrpConfig, ast.configuration)
    scaleGrpConfig.minSize = ast.configuration.minSize
    scaleGrpConfig.maxSize = ast.configuration.maxSize

    val scaleGroup = new ScaleGroupInstance
    scaleGrpConfig.parentScaleGroupInstance = scaleGroup
    scaleGroup.localName = ast.name
    scaleGroup.configuration  = scaleGrpConfig
    scaleGroup.triggers = compileTriggers(ast.triggers)
    scaleGroup.actions = compile(ast.actions)
    scaleGroup
  }

  private def compileTriggers(astTriggers: List[ASTTrigger]) = {
    astTriggers.map(createTriggerInstance(_))    
  }

  private def createTriggerInstance(astTrigger: ASTTrigger) = {
    val triggerInstance = new TriggerInstance
    triggerInstance.name = astTrigger.name
    triggerInstance.breachDuration = astTrigger.breachDuration
    triggerInstance.upperBreachIncrement = astTrigger.upperBreachIncrement
    triggerInstance.lowerBreachIncrement = astTrigger.lowerBreachIncrement
    triggerInstance.lowerThreshold = astTrigger.lowerThreshold
    triggerInstance.measure = astTrigger.measure
    triggerInstance.namespace = astTrigger.namespace
    triggerInstance.period = astTrigger.period
    triggerInstance.statistic = astTrigger.statistic
    triggerInstance.upperThreshold = astTrigger.upperThreshold
    triggerInstance.unit = astTrigger.unit
    triggerInstance
  }

}