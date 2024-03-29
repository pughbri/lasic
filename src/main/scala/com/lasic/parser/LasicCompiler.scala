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
  var blockComment = """(/\*([^*]|(\*+[^*/]))*\*+/)|([^:]//.*)""".r

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
        val concreteNodes = ast.nodes.toList filter (!_.isAbstract)
        systemInstance.nodegroups = concreteNodes map {
          case node =>
            val nodeGroup: NodeGroup = compile(node, ast.nodes.toList);
            nodeGroup.parentSystemInstance = systemInstance
            nodeGroup
        }
    }
  }

  private def createScaleGroups(ast: ASTSystem, sysGroup: SystemGroup) = {
    sysGroup.instances.foreach {
      systemInstance =>
        systemInstance.scaleGroups = ast.scaleGroups.toList.map {
          case astScaleGroup =>
            val scaleGroup = compile(astScaleGroup)
            scaleGroup.parentSystemInstance = systemInstance
            scaleGroup
        }
    }
  }


  private def createLoadBalancers(ast: ASTSystem, sysGroup: SystemGroup) = {
    sysGroup.instances.foreach {
      systemInstance =>
        systemInstance.loadBalancers = ast.loadBalancers.toList.map {
          case astLoadBalancer =>
            val loadBalancer = compile(astLoadBalancer)
            loadBalancer.parentSystemInstance = systemInstance
            loadBalancer
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

    createLoadBalancers(ast, sysGroup)

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
            case scaleGroupInstance: ScaleGroupInstance => scaleGroupInstance.cloudName = boundPath._2
            case scaleGroupConfig: ScaleGroupConfiguration => scaleGroupConfig.cloudName = boundPath._2
            case loadBalancerInstance: LoadBalancerInstance => loadBalancerInstance.cloudName = boundPath._2
            case volumeInstance: VolumeInstance => volumeInstance.id = boundPath._2
            case _ => throw new Exception("Path " + boundPath + " does not resolve to a nodeinstance.  Resolves to " + pathables(0).getClass)
          }
        }

      }
    }
  }

  def copyNodeProperties(to: NodeProperties, from: NodeProperties): Unit = {
    to.copySetProperties(from)
  }

  private def compile(ast: ASTNode, allAstNodes: List[ASTNode]): NodeGroup = {
    //find the parent node if it exists
    ast.parentNode match {
      case Some(parentNodeName) => {
        val parentNodeAst = allAstNodes find (_.name == parentNodeName)
        parentNodeAst match {
          case Some(parentNode) => compile(ast,parentNodeAst)
          case None => throw new CompilationFailureException("unknown parent node '" + parentNodeName + "' on node '" + ast.name + "'")
        }
      }
      case None => compile(ast, None)
    }
  }

  private def compile(ast: ASTNode, parentAstOption: Option[ASTNode]): NodeGroup = {
    val nodeGroup = new NodeGroup
    var baseActions = List[Action]()
    parentAstOption match {
      case Some(parentAst) => {  //set the values from the base node
        copyNodeProperties(nodeGroup, parentAst)
        baseActions = compile(parentAst.actions)
        nodeGroup.loadBalancers = parentAst.loadBalancers
      }
      case None => //no base node so do nothing
    }

    copyNodeProperties(nodeGroup, ast)
    val subNodeActions = compile(ast.actions)

    //add the "baseactions" that aren't "overridden" to the node groups action list
    baseActions foreach {
      action => {
        subNodeActions find (_.name == action.name) match {
          case Some(x) => // exists in subnode so is "overridden"
          case None => nodeGroup.actions = action :: nodeGroup.actions
        }
      }

    }
    nodeGroup.actions = nodeGroup.actions ::: subNodeActions
    nodeGroup.loadBalancers = nodeGroup.loadBalancers ::: ast.loadBalancers
    createInstances(nodeGroup, ast)
    nodeGroup
  }

  private def compile(actions: List[BaseAction]): List[Action] = {
    actions map {
      case astAction: ASTAction => {
        val action = new Action()
        action.name = astAction.name
        action.scpMap = astAction.scpMap
        action.scriptDefinitions = astAction.scriptDefinitions
        action.ipMap = astAction.ipMap
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
    scaleGroup.configuration = scaleGrpConfig
    scaleGroup.triggers = compileTriggers(ast.triggers)
    scaleGroup.actions = compile(ast.actions)
    scaleGroup.loadBalancers = ast.loadBalancers
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

  private def compile(ast: ASTLoadBalancer): LoadBalancerInstance = {
    val loadBalancerInstance = new LoadBalancerInstance
    loadBalancerInstance.localName = ast.localName
    loadBalancerInstance.lbPort = ast.lbPort
    loadBalancerInstance.instancePort = ast.instancePort
    loadBalancerInstance.protocol = ast.protocol
    loadBalancerInstance.sslcertificate = ast.sslcertificate
    loadBalancerInstance
  }

}