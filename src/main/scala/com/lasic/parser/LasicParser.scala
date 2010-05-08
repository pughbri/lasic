package com.lasic.parser

import util.parsing.combinator.syntactical.StdTokenParsers
import util.parsing.combinator.lexical.StdLexical
import util.parsing.combinator.JavaTokenParsers

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

  def system = "system" ~ lbrace ~ system_internals ~ rbrace

  def system_internals = rep(system_internals_1)

  def system_internals_1: Parser[Any] = name | count | node | system

  def name = "name" ~ "=" ~ stringLiteral

  def count = "count" ~ eq ~ wholeNumber

  def node = "node" ~ lbrace ~ node_internals ~ rbrace

  def node_internals = rep(name | count | machineimage | kernelid | ramdiskid | groups | key | user | instancetype | scripts | scp)

  def machineimage = "machineimage" ~ eq ~ stringLiteral

  def kernelid = "kernelid" ~ eq ~ stringLiteral

  def ramdiskid = "ramdiskid" ~ eq ~ stringLiteral

  def groups = "groups" ~ eq ~ stringLiteral

  def key = "key" ~ eq ~ stringLiteral

  def user = "user" ~ eq ~ stringLiteral

  def instancetype = "instancetype" ~ eq ~ stringLiteral

  def scripts = "scripts" ~ peq ~ stringLiteral ~ script_args

  def script_args = (lbrace ~ rbrace) | (lbrace ~ rep(ident ~ eq ~ stringLiteral) ~ rbrace)

  def scp = "scp" ~ peq ~ stringLiteral ~ ":" ~ stringLiteral

}

object Foo {
  def main(args: Array[String]) {
    val p = new LasicParser
    var result = p.parseAll(p.system, "system { name=\"foo\" }");
    println(result)
  }

}
