package com.lasic.interpreter

import com.lasic.Cloud
import com.lasic.model.LasicProgram

/**
 * Created by IntelliJ IDEA.
 * User: lmonson
 * Date: Jun 7, 2010
 * Time: 2:05:30 PM
 * To change this template use File | Settings | File Templates.
 */

trait Verb {
  val cloud:Cloud
  val program:LasicProgram
  def doit
}