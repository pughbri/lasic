package com.lasic.cloud.ssh

import junit.framework.TestCase
import java.io.File
import java.lang.String

/**
 *
 * @author Brian Pugh
 */

class BashPreparedScriptExecutionTest extends TestCase("BashPreparedScriptExecutionTest") {

  def testEnvVarsAsShellCmd() = {
    val numbers = List("one", "two", "three")
    val letters = List("a")
    val envVars = Map("numbers" -> numbers, "letters" -> letters)
    val preparedScriptExecution = new BashPreparedScriptExecution(null, "myscript.sh", envVars)
    val expectedString = "export numbers=\"one two three\" && export letters=\"a\""
    assert(preparedScriptExecution.envVarsAsShellCmd == expectedString, "expected " + expectedString + " but got " + preparedScriptExecution.envVarsAsShellCmd)
  }



  class MockSshSession extends SshSession("dns", "ubuntu", new File("")) {
    var firstCommandOk = false
    var secondCommandOk = false

    override def sendCommand(cmd: String) = {
      val expected1 = "sudo chmod +x myscript.sh"
      val expected2 = """. /etc/profile ; export numbers="one two three" && export letters="a" && myscript.sh 2>&1"""
      cmd match {
        case `expected1` => firstCommandOk = true
        case `expected2` => secondCommandOk = true
        case _ => assert(false,"unexpected cmd: " + cmd)
      }
      0
    }
  }

  def testExecute() = {
    val session = new MockSshSession()
    val numbers = List("one", "two", "three")
    val letters = List("a")
    val envVars = Map("numbers" -> numbers, "letters" -> letters)
    val preparedScriptExecution = new BashPreparedScriptExecution(session, "myscript.sh", envVars)
    preparedScriptExecution.execute   //asserts are in the MockSshSession
  }


}