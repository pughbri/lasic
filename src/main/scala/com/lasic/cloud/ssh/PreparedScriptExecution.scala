package com.lasic.cloud.ssh

import com.lasic.util.Logging

/**
 *
 * @author Brian Pugh
 */

abstract class PreparedScriptExecution(session: SshSession, script: String, envVars: Map[String, List[String]]) extends Logging {

  /**
   * Calls prepareRemoteScript, createCommand,createCmdWithEnvironmentValsSet, and loadProfile then executes script
   * using session 
   */
  def execute(): Int = {
    prepareRemoteScript
    var cmd = createCommand(script)
    cmd = createCmdWithEnvironmentValsSet(cmd, envVars)
    cmd = loadProfile(cmd)
    session.sendCommand(cmd)
  }

  /**
   * Add setting of needed environment variables to cmd.  
   */
  protected def createCmdWithEnvironmentValsSet(cmd: String, envVars: Map[String, List[String]]): String

  /**
   * create an executable command from the script.
   */
  protected def createCommand(script: String): String

  /**
   *  Return a command that will load a profile and execute cmd.
   */
  protected def loadProfile(cmd: String): String

  /**
   * perform any needed operation on the remote script.
   */
  protected def prepareRemoteScript()


}