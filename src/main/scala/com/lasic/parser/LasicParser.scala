package com.lasic.parser

import ast._
import util.parsing.combinator.JavaTokenParsers
import scala.collection.mutable._
import com.lasic.LasicProperties
import com.lasic.model.{ScriptArgumentValue, PathScriptArgumentValue, LiteralScriptArgumentValue}
import com.lasic.util.Logging

/**
 * Created by IntelliJ IDEA.
 * User: lmonson
 * Date: May 6, 2010
 * Time: 10:20:08 PM
 * To change this template use File | Settings | File Templates.
 */

class LasicParser extends JavaTokenParsers with Logging{
  /*==========================================================================================================
    NON BNF utility methods
   ==========================================================================================================*/
  def buildSystem(name: String, body: List[Any]) = {
    val sys = new ASTSystem()
    sys.name = name
    body.foreach {
      listEntry =>
        listEntry match {
          case propertyMap: Map[Any, Any] => initSystemProperties(sys, propertyMap)
          case node: ASTNode => sys.nodes += node
          case system: ASTSystem => sys.subsystems += system
          case x => logger.warn("Unknown object: " + x)
        }
    }
    sys
  }

  def initSystemProperties(sys: ASTSystem, props: Map[Any, Any]) {
    props.foreach {
      case ("count", v: Int) => sys.count = v
    }
  }

  def buildNode(name: String, body: List[Any]) = {
    val sys = new ASTNode()
    sys.name = name
    body.foreach {
      listEntry =>
        listEntry match {
          case propertyMap: Map[Any, Any] => initNodeProperties(sys, propertyMap)
          case astScp: ASTScp => sys.scpMap = scala.collection.Map.empty ++ astScp.scpMap
          case astScript: ASTScript =>
              sys.scriptMap = scala.collection.Map.empty ++ astScript.scpMap.map {
                tuple =>  (tuple._1, scala.collection.Map.empty ++ tuple._2 )

              }
          case astVolume: ASTVolume =>
              val immutableMap = scala.collection.Map.empty ++ astVolume.params
              sys.volumes= immutableMap :: sys.volumes 
          case _ =>
        }
    }
    sys
  }

  def initNodeProperties(sys: ASTNode, props: Map[Any, Any]) {
    props.foreach {
      case ("count", v: Int) => sys.count = v
      case ("machineimage", s: String) => sys.machineimage = s
      case ("kernelid", s: String) => sys.kernelid = s
      case ("ramdiskid", s: String) => sys.ramdiskid = s
      case ("groups", s: List[String]) => sys.groups = s
      case ("key", s: String) => sys.key = s
      case ("user", s: String) => sys.user = s
      case ("instancetype", s: String) => sys.instancetype = s
      case (x, y) => logger.warn("Unknown node property: " + x + " = " + y)
    }

  }
  /*==========================================================================================================
    SYSTEM bnf
    ==========================================================================================================*/
  def system = "system" ~ aString ~ lbrace ~ system_body ~ rbrace ^^ {
    case _ ~ name ~ _ ~ body_list ~ _ => buildSystem(name, body_list)
  }

  def system_body: Parser[List[Any]] = rep(system_props | node | system)

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
    NODE bnf
    ==========================================================================================================*/


  def node = "node" ~ aString ~ lbrace ~ node_body ~ rbrace ^^ {
    case _ ~ name ~ _ ~ body_list ~ _ => buildNode(name, body_list)

  }

  def node_body = rep(node_props | scripts | scp | volume)

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

  def scripts = "scripts" ~ lbrace ~ scripts_body ~ rbrace ^^ {
    case _ ~ _ ~ list_o_scripts ~ _ =>
      val s = new ASTScript
      s.scpMap = Map() ++ list_o_scripts
      s
  }

  def scripts_body = rep(script_stmnt)

  def script_stmnt = aString ~ ":" ~ lbrace ~ rep(script_param) ~ rbrace ^^ {
    case name ~ _ ~ _ ~ arg_list ~ _ =>
      val argMap = Map[String,ScriptArgumentValue]() ++ arg_list
      (name -> argMap)
  }

  def script_param:Parser[Tuple2[String,ScriptArgumentValue]] = script_param_literal | script_param_path

  def script_param_literal = ident ~ ":" ~ aString ^^ {
    case from ~ _ ~ to =>
      (from -> new LiteralScriptArgumentValue(to))
  }

  def path:Parser[String] = """/(((system|node)\['[a-zA-Z0-9 -_]+'\](\[[0-9]+\])?)|/)*""".r
//  def path:Parser[String] = """/((system\['[a-zA-Z0-9 -_]+'\])|/)*""".r

  def script_param_path = ident ~ ":" ~ path ^^ {
    case from ~ _ ~ to =>
      (from -> new PathScriptArgumentValue(to))
  }



  def scp = "scp" ~ lbrace ~ scp_body ~ rbrace ^^ {
    case _ ~ _ ~ list_body ~ _ =>
      val x = new ASTScp
      x.scpMap = Map() ++ list_body
      x
  }

  def scp_body = rep(scp_line)

  def scp_line = aString ~ ":" ~ aString ^^ {case from ~ _ ~ to => (from -> to)}

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

