package com.lasic.model
import org.apache.commons.io.IOUtils
import com.lasic.parser.LasicCompiler

/**
 *
 * @author Brian Pugh
 */

trait ProgramLoader {
  def getLasicProgram(i: Int) = {
    val path = "/model/Program%03d.lasic".format(i)
    val is = getClass.getResourceAsStream(path)
    val program = IOUtils.toString(is);
    LasicCompiler.compile(program)
  }
}
