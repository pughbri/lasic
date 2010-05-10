package com.lasic.parser

import util.parsing.combinator.syntactical.StdTokenParsers
import util.parsing.combinator.lexical.StdLexical
import util.parsing.combinator.JavaTokenParsers
import util.matching.Regex

/**
 * Created by IntelliJ IDEA.
 * User: lmonson
 * Date: May 6, 2010
 * Time: 10:20:08 PM
 * To change this template use File | Settings | File Templates.
 */

class LasicParser extends JavaTokenParsers {

  def lbrace = "{"

  def rbrace = "}"

  def eq = "="

  def peq = "+="

  def system = "system" ~ stringLiteral ~ lbrace ~ system_body ~ rbrace

  def system_body: Parser[Any] = rep(props | node | system)

  def props = "props" ~ lbrace ~ props_body ~ rbrace

  def props_body = rep(single_prop)

  def single_prop = ident ~ ":" ~ (wholeNumber | stringLiteral)

  def node = "node" ~ stringLiteral ~ lbrace ~ node_body ~ rbrace

  def node_body: Parser[Any] = rep(props | scripts | scp)

  def scripts = "scripts" ~ lbrace ~ scripts_body ~ rbrace

  def scripts_body = rep(script_stmnt)

  def script_stmnt = stringLiteral ~ ":" ~ lbrace ~ rep(script_param) ~ rbrace

  def script_param = ident ~ ":" ~ stringLiteral

  def scp = "scp" ~ lbrace ~ scp_body ~ rbrace

  def scp_body = rep(stringLiteral ~ ":" ~ stringLiteral)


}

object Foo {
  def main(args: Array[String]) {
//    val p = new LasicParser
//    var result = p.do_parse("system { name=\"foo\" }");
//    println(result)
  }

}
