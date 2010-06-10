package com.lasic.cloud.ssh

/**
 *
 * @author Brian Pugh
 */

class BashPreparedScriptExecution(session: SshSession,
                                  script: String,
                                  envVars: Map[String, List[String]])  extends PreparedScriptExecution(session, script, envVars) {
  /**
   * Adds export statements for variables to cmd
   */
  def createCmdWithEnvironmentValsSet(cmd: String, envVars: Map[String, List[String]]) = {
    if (envVars.size > 0 && envVarsAsShellCmd.length > 0) {
      envVarsAsShellCmd + " && " + cmd
    }
    else {
      cmd
    }
  }

  /**
   * Executes script redirecting stdout and stderr
   */
  def createCommand(script: String) = {
    script + " 2>&1"
  }

  /**
   *  Sources /etc/profile then executes cmd.
   */
  def loadProfile(cmd: String) = {
    ". /etc/profile ; " + cmd
  }

  /**
   * Sets execute permission on script
   */
  def prepareRemoteScript() = {

    var chmodCommand: String = prefaceWithSudo("chmod +x " + script)
    val result = session.sendCommand(chmodCommand)
    if (result != 0) {
      logger.warn("Error {} executing chmod on {}@{}:{}", Array[Any](result, session.userName, session.dnsName, script))
    }
  }

  def envVarsAsShellCmd: String = {
    var cmd = ""
    for (key <- envVars.keySet) {
      var sb: StringBuilder = new StringBuilder
      envVars(key).foreach(value => {
        sb.append(value)
        sb.append(" ")
      })
      var values = sb.toString.trim
      if (values.length > 0) cmd = cmd + "export " + key + "=\"" + values + "\" && "
    }
    if (cmd.length == 0) return ""
    var idx: Int = cmd.lastIndexOf(" && ")
    cmd.substring(0, idx)
  }

  protected def prefaceWithSudo(cmd: String): String = {
    (if ((!session.userName.equals("root"))) "sudo " else "") + cmd
  }


}