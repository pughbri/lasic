package com.lasic.parser

import ast._
import util.parsing.combinator.JavaTokenParsers
import scala.collection.mutable._
import com.lasic.LasicProperties
import com.lasic.model.{ArgumentValue, PathArgumentValue, LiteralArgumentValue}
import com.lasic.util.Logging
import com.lasic.values.NodeProperties

/**
 * Created by IntelliJ IDEA.
 * User: lmonson
 * Date: May 6, 2010
 * Time: 10:20:08 PM
 * To change this template use File | Settings | File Templates.
 */

class LasicParser extends JavaTokenParsers with Logging {
  /*==========================================================================================================
    NON BNF utility methods
   ==========================================================================================================*/
  def buildSystem(name: String, body: List[Any], paths: Any) = {
    val sys = new ASTSystem()
    sys.name = name
    body.foreach {
      listEntry =>
        listEntry match {
          case propertyMap: Map[Any, Any] => initSystemProperties(sys, propertyMap)
          case node: ASTNode => sys.nodes += node
          case scaleGroup: ASTScaleGroup => sys.scaleGroups += scaleGroup
          case system: ASTSystem => sys.subsystems += system
          case loadBalancer: ASTLoadBalancer => sys.loadBalancers += loadBalancer
          case x => logger.warn("Unknown object: " + x)
        }
    }
    paths match {
      case pathMap: Map[String, String] => sys.boundPaths = sys.boundPaths ++ pathMap
      case _ =>
    }
    sys
  }

  def initSystemProperties(sys: ASTSystem, props: Map[Any, Any]) {
    props.foreach {
      case ("count", v: Int) => sys.count = v
    }
  }

  def buildScaleGroup(name: String, body: List[Any]) = {
    val scaleGroup = new ASTScaleGroup()
    scaleGroup.name = name
    body.foreach {
      listEntry =>
        listEntry match {
          case propertyMap: Map[Any, Any] => scaleGroup.configuration = createScaleGroupConfiguration(propertyMap)
          case astAction: ASTAction => scaleGroup.actions = astAction :: scaleGroup.actions
          case astVolume: ASTVolume =>
            val immutableMap = scala.collection.Map.empty ++ astVolume.params
            scaleGroup.volumes = immutableMap :: scaleGroup.volumes
          case astTrigger: ASTTrigger => scaleGroup.triggers = astTrigger :: scaleGroup.triggers
          case loadBalancers: List[ArgumentValue] => scaleGroup.loadBalancers = loadBalancers ::: scaleGroup.loadBalancers
          case _ =>
        }
    }
    scaleGroup
  }

  def createScaleGroupConfiguration(props: Map[Any, Any]) = {
    val config = new ASTScaleGroupConfig
    config.minSize = props("min-size").asInstanceOf[Int]
    config.maxSize = props("max-size").asInstanceOf[Int]
    config.name = props("name").asInstanceOf[String]
    initNodeProperties(config, props -- List("min-size", "max-size", "name"))
    config
  }

  def createTrigger(name: String, triggerProps: Map[String, Any]) = {
    val astTrigger = new ASTTrigger
    astTrigger.name = name
    triggerProps.foreach {
      case ("breach-duration", i: Int) => astTrigger.breachDuration = i
      case ("upper-breach-increment", i: Int) => astTrigger.upperBreachIncrement = i
      case ("lower-breach-increment", i: Int) => astTrigger.lowerBreachIncrement = i
      case ("lower-threshold", i: Int) => astTrigger.lowerThreshold = i
      case ("measure", s: String) => astTrigger.measure = s
      case ("namespace", s: String) => astTrigger.namespace = s
      case ("period", i: Int) => astTrigger.period = i
      case ("statistic", s: String) => astTrigger.statistic = s
      case ("upper-threshold", i: Int) => astTrigger.upperThreshold = i
      case ("unit", s: String) => astTrigger.unit = s
      case (x, y) => logger.warn("Unknown node property: " + x + " = " + y)
    }
    astTrigger
  }

  def buildNode(name: String, body: List[Any]) = {
    val node = new ASTNode()
    node.name = name
    body.foreach {
      listEntry =>
        listEntry match {
          case propertyMap: Map[Any, Any] => initNodeProperties(node, propertyMap)
          case astAction: ASTAction => node.actions = astAction :: node.actions
          case astVolume: ASTVolume =>
            val immutableMap = scala.collection.Map.empty ++ astVolume.params
            node.volumes = immutableMap :: node.volumes
          case loadBalancers: List[ArgumentValue] => node.loadBalancers = loadBalancers ::: node.loadBalancers
          case _ =>
        }
    }
    node
  }

  def buildAction(actionName: String, body: List[Any]) = {
    val action = new ASTAction
    action.name = actionName
    body.foreach {
      listEntry =>
        listEntry match {
          case astScp: ASTScp => action.scpMap = action.scpMap ++ astScp.scpMap
          case astScript: ASTScript =>
            action.scriptMap = action.scriptMap ++ astScript.scpMap.map {
              tuple => (tuple._1, scala.collection.Map.empty ++ tuple._2)

            }
          case astIp: ASTIp => action.ipMap = action.ipMap ++ astIp.ipMap
          case _ =>
        }
    }
    action
  }

  def initNodeProperties(nodeProps: NodeProperties, props: Map[Any, Any]) {
    props.foreach {
      case ("count", v: Int) => nodeProps.count = v
      case ("machineimage", s: String) => nodeProps.machineimage = s
      case ("kernelid", s: String) => nodeProps.kernelid = s
      case ("ramdiskid", s: String) => nodeProps.ramdiskid = s
      case ("groups", s: List[String]) => nodeProps.groups = s
      case ("key", s: String) => nodeProps.key = s
      case ("user", s: String) => nodeProps.user = s
      case ("instancetype", s: String) => nodeProps.instancetype = s
      case (x, y) => logger.warn("Unknown node property: " + x + " = " + y)
    }
  }

  def buildLoadBalancer(name: String, body: List[Tuple2[String, Any]]) = {
    val loadBalancer = new ASTLoadBalancer()
    loadBalancer.localName = name
    body.foreach {
      listEntry =>
        listEntry match {
          case ("lb-port", i: Int) => loadBalancer.lbPort = i
          case ("instance-port", i: Int) => loadBalancer.instancePort = i
          case ("protocol", s: String) => loadBalancer.protocol = s
          case ("sslcertificate", s: String) => loadBalancer.sslcertificate = s
          case (x, y) => logger.warn("Unknown load balancer property: " + x + " = " + y)
        }
    }
    loadBalancer
  }


  /*==========================================================================================================
    SYSTEM bnf
    ==========================================================================================================*/
  def system = "system" ~ aString ~ lbrace ~ system_body ~ rbrace ^^ {
    case _ ~ name ~ _ ~ body_tuple ~ _ => buildSystem(name, body_tuple._1, body_tuple._2)
  }

  def system_body: Parser[(List[Any], Any)] = rep(system_props | node | scale_group | load_balancer | system) ~ opt(path_bindings) ^^ {
    case list_o_declarations ~ Some(x) => (list_o_declarations, x)
    case list_o_declarations ~ None => (list_o_declarations, None)
  }


  def system_props = "props" ~> "{" ~> rep(system_prop) <~ "}" ^^ {
    list_o_props => Map() ++ list_o_props
  }

  def system_prop = system_numeric_prop

  def system_numeric_prop = system_numeric_prop_name ~ ":" ~ wholeNumber ^^ {
    case name ~ _ ~ value =>
      (name -> value.toInt)
  }

  def system_numeric_prop_name = "count"

  /*==========================================================================================================
    Paths bnf
    ==========================================================================================================*/

  def path_bindings = "paths" ~ lbrace ~ path_body ~ rbrace ^^ {
    case _ ~ _ ~ bindings ~ _ => bindings
  }

  def path_body = rep(path_binding) ^^ {
    list_o_bindings => Map() ++ list_o_bindings
  }

  def path_binding = path ~ ":" ~ aString ^^ {
    case path_name ~ _ ~ identifier => (path_name -> identifier)
  }


  /*==========================================================================================================
  SCALE GROUP bnf
  ==========================================================================================================*/
  def scale_group = "scale-group" ~ aString ~ lbrace ~ scale_group_body ~ rbrace ^^ {
    case _ ~ name ~ _ ~ body_list ~ _ => buildScaleGroup(name, body_list)
  }

  def scale_group_body = rep(scale_config | trigger | action | volume | load_balancers)

  def scale_config = "configuration" ~ aString ~ lbrace ~ rep(scale_group_prop) ~ rbrace ^^ {
    case _ ~ name ~ _ ~ list_o_props ~ _ => Map() ++ list_o_props + ("name" -> name)
  }

  def scale_group_prop = scale_group_numeric_prop | node_string_prop | node_list_prop

  def scale_group_numeric_prop = scale_group_numeric_prop_name ~ ":" ~ wholeNumber ^^ {
    case name ~ _ ~ value => (name -> value.toInt)
  }

  def scale_group_numeric_prop_name = "min-size" | "max-size"

  def trigger = "scale-trigger" ~ aString ~ lbrace ~ trigger_body ~ rbrace ^^ {
    case _ ~ name ~ _ ~ body ~ _ => createTrigger(name, body)
  }

  def trigger_body = rep(trigger_numeric_prop | trigger_string_prop) ^^ {
    list_o_props => Map() ++ list_o_props
  }

  def trigger_numeric_prop = trigger_numeric_prop_name ~ ":" ~ wholeNumber ^^ {
    case name ~ _ ~ value => (name -> value.toInt)
  }

  def trigger_numeric_prop_name = "breach-duration" | "upper-breach-increment" | "lower-breach-increment" | "lower-threshold" | "period" | "upper-threshold"

  def trigger_string_prop = trigger_string_prop_name ~ ":" ~ aString ^^ {
    case name ~ _ ~ value => (name -> value)
  }

  def trigger_string_prop_name = "measure" | "namespace" | "statistic" | "unit"

  def load_balancers = "load-balancers" ~ lbrace ~ load_balancers_body ~ rbrace ^^ {
    case _ ~ _ ~ list_of_loadbalancers ~ _ => list_of_loadbalancers
  }


  def load_balancers_body = rep(load_balancer_entry)

  def load_balancer_entry = literal | path_value

  def literal = aString ^^ {
    case x => new LiteralArgumentValue(x)
  }

  def path_value = path ^^ {
    case x => new PathArgumentValue(x)
  }

  /*==========================================================================================================
  NODE bnf
  ==========================================================================================================*/


  def node = "node" ~ aString ~ lbrace ~ node_body ~ rbrace ^^ {
    case _ ~ name ~ _ ~ body_list ~ _ => buildNode(name, body_list)
  }

  def node_body = rep(node_props | action | volume | load_balancers)

  def node_props = "props" ~> "{" ~> rep(node_prop) <~ "}" ^^ {
    list_o_props => Map() ++ list_o_props
  }

  def node_prop = node_numeric_prop | node_string_prop | node_list_prop

  def node_numeric_prop = node_numeric_prop_name ~ ":" ~ wholeNumber ^^ {
    case name ~ _ ~ value => (name -> value.toInt)
  }

  def node_numeric_prop_name = "count"

  def node_string_prop = node_string_prop_name ~ ":" ~ aString ^^ {
    case name ~ _ ~ value => (name -> value)
  }

  def node_string_prop_name = "count" | "machineimage" | "kernelid" | "ramdiskid" | "key" | "user" | "instancetype"

  def node_list_prop = node_list_prop_name ~ ":" ~ repsep(aString, ",") ^^ {
    case name ~ _ ~ value_list => (name -> value_list)
  }

  def node_list_prop_name = "groups"

  def action = "action" ~ aString ~ lbrace ~ rep(scripts | scp | ips) ~ rbrace ^^ {
    case _ ~ actionName ~ _ ~ list_o_cmds ~ _ =>
      buildAction(actionName, list_o_cmds)
  }

  def scripts = "scripts" ~ lbrace ~ scripts_body ~ rbrace ^^ {
    case _ ~ _ ~ list_o_scripts ~ _ =>
      val s = new ASTScript
      s.scpMap = Map() ++ list_o_scripts
      s
  }

  def scripts_body = rep(script_stmnt)

  def script_stmnt = aString ~ ":" ~ lbrace ~ rep(script_param) ~ rbrace ^^ {
    case name ~ _ ~ _ ~ arg_list ~ _ =>
      val argMap = Map[String, ArgumentValue]() ++ arg_list
      (name -> argMap)
  }

  def script_param: Parser[Tuple2[String, ArgumentValue]] = script_param_literal | script_param_path

  def script_param_literal = ident ~ ":" ~ aString ^^ {
    case from ~ _ ~ to =>
      (from -> new LiteralArgumentValue(to))
  }

  def path: Parser[String] = """/(((system|node)\['[a-zA-Z0-9 -_]+'\](\[[0-9]+\])?)|/)*""".r
  //  def path:Parser[String] = """/((system\['[a-zA-Z0-9 -_]+'\])|/)*""".r

  def script_param_path = ident ~ ":" ~ path ^^ {
    case from ~ _ ~ to =>
      (from -> new PathArgumentValue(to))
  }


  def scp = "scp" ~ lbrace ~ scp_body ~ rbrace ^^ {
    case _ ~ _ ~ list_body ~ _ =>
      val x = new ASTScp
      x.scpMap = Map() ++ list_body
      x
  }

  def scp_body = rep(scp_line)

  def scp_line = aString ~ ":" ~ aString ^^ {case from ~ _ ~ to => (from -> to)}

  def ips = "ips" ~ lbrace ~ ip_body ~ rbrace ^^ {
    case _ ~ _ ~ list_body ~ _ =>
      val x = new ASTIp
      x.ipMap = Map() ++ list_body
      x
  }

  def ip_body = rep(ip_line)

  def ip_line = "node[" ~ aNumber ~ "]:" ~ aString ^^ {
    case _ ~ idx ~ _ ~ ip_address => (idx -> ip_address)
  }

  def volume = "volume" ~ aString ~ lbrace ~ volume_body ~ rbrace ^^ {
    case _ ~ name ~ _ ~ vol_body ~ _ =>
      val astVolume = new ASTVolume
      astVolume.params = vol_body += ("name" -> name)
      astVolume
  }

  def volume_body = rep(volume_param) ^^ {
    case params => Map() ++ params
  }

  def volume_param = {"size" | "device" | "mount"} ~ ":" ~ aString ^^ {case label ~ _ ~ value => (label -> value)}

  /*==========================================================================================================
  load-balancer bnf
  ==========================================================================================================*/


  def load_balancer = "load-balancer" ~ aString ~ lbrace ~ load_balancer_body ~ rbrace ^^ {
    case _ ~ name ~ _ ~ body_list ~ _ => buildLoadBalancer(name, body_list)
  }

  def load_balancer_body = load_balancer_props ^^ {
    map_o_props => List() ++ map_o_props
  }

  def load_balancer_props = "props" ~> "{" ~> rep(load_balancer_prop) <~ "}" ^^ {
    list_o_props => Map() ++ list_o_props
  }

  def load_balancer_prop = load_balancer_numeric_prop | load_balancer_string_prop | load_balancer_protocol_prop

  def load_balancer_numeric_prop = load_balancer_numeric_prop_name ~ ":" ~ wholeNumber ^^ {
    case name ~ _ ~ value => (name -> value.toInt)
  }

  def load_balancer_numeric_prop_name = "lb-port" | "instance-port"

  def load_balancer_string_prop = load_balancer_string_prop_name ~ ":" ~ aString ^^ {
    case name ~ _ ~ value => (name -> value)
  }

  def load_balancer_string_prop_name = "sslcertificate"

  def load_balancer_protocol_prop = "protocol" ~ ":" ~ protocols ^^ {
    case _ ~ _ ~ value => ("protocol" -> value)
  }

  def protocols = "TCP" | "HTTPS" | "HTTP" | "SSL"


  /*==========================================================================================================
   Misc bnf
  ==========================================================================================================*/
  def aString = stringLiteral ^^ {
    x =>
      val y = LasicProperties.resolveProperty(x);
      val z = y.substring(1, y.length - 1)
      z
  }

  def aNumber = wholeNumber ^^ {_.toInt}

  def lbrace = "{"

  def rbrace = "}"

  def eq = "="

  def peq = "+="


}

