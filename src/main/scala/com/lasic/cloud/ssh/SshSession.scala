package com.lasic.cloud.ssh

import com.jcraft.jsch._
import java.io._
import com.lasic.util.Logging

/**
 *
 * User: Brian Pugh
 * Date: May 13, 2010
 */

class SshSession(val dnsName: String, val userName: String, val pemFile: File) extends JSch with Logging {
  require(dnsName != null && !dnsName.isEmpty, "dnsName must be provided")
  require(userName!= null && !userName.isEmpty, "userName must be provided")
  require(pemFile != null, "pemFile must be provided")

  private var isConnected: Boolean = false

  //TODO: something real with output handling
  private val output: OutputStream = System.out

  private var session: Session = null


  def connect(): Unit = {
    if (isConnected) {
      throw new ConnectException("Attempted to connect to " + dnsName + " but this SSH session is already in use")
    }
    try {
      addIdentity(pemFile.getAbsolutePath)
      session = getSession(userName, dnsName)
      session.setUserInfo(new MySshUserInfo)
      session.setConfig("StrictHostKeyChecking", "no")

      // Hack! todo: fix this.. this is a terrible way to do this
      if (System.getProperties.containsKey("ssh_proxy")) {
        var proxy: Proxy = new ProxyHTTP(System.getProperties.get("ssh_proxy").toString, 80)
        session.setProxy(proxy)
      }

      session.connect
      isConnected = true
    }
    catch {
      case e: JSchException => {
        e.getMessage match {
          case "Auth fail" => throw new AuthFailureException("Authentication failed.  Please check your credentials:  Username ["
                  + userName + "] Host [" + dnsName + "] key [" + pemFile.getAbsolutePath + "]", e)
          case _ =>
        }

        e.getCause match {
          case e: FileNotFoundException => throw new AuthFailureException("Authentication failed.  Key file not found", e)
          case _ => throw new ConnectException(e.getMessage, e)
        }
      }
    }
  }


  def disconnect: Unit = {
    try {
      removeAllIdentity
      if (session != null && session.isConnected) {
        session.disconnect
      }
    }
    catch {
      case e: Exception => {
        throw new RuntimeException("Error received trying to disconnect", e)
      }
    }
    isConnected = false
  }


  def sendFile(f: File, remoteFileName: String): Int = {
    require(isConnected, "connect must be called before session can be used")
    try {
      logger.debug("Sending local file {} to remote machine as {}", f.getCanonicalFile, remoteFileName)
      var fis: FileInputStream = null
      var command: String = "scp -p -t " + remoteFileName
      var channel: Channel = session.openChannel("exec")
      (channel.asInstanceOf[ChannelExec]).setCommand(command)
      var out: OutputStream = channel.getOutputStream
      var in: InputStream = channel.getInputStream
      channel.connect
      if (checkAck(in) != 0) {
        throw new RuntimeException("Send file failed: checkAck returned non-zero")
      }
      var filesize: Long = f.length
      command = "C0644 " + filesize + " " + f.getName
      command += "\n"
      out.write(command.getBytes)
      out.flush
      if (checkAck(in) != 0) {
        throw new RuntimeException("Send file failed: checkAck returned non-zero")
      }
      fis = new FileInputStream(f)
      var buf = new Array[Byte](1024)
      var continue = true;
      while (continue) {
        var len: Int = fis.read(buf, 0, buf.length)
        if (len <= 0) {
          continue = false
        }
        else {
          out.write(buf, 0, len)
        }
      }
      fis.close
      fis = null
      buf(0) = 0
      out.write(buf, 0, 1)
      out.flush
      if (checkAck(in) != 0) {
        System.exit(0)
      }
      out.close
      channel.disconnect
      channel.getExitStatus
    }
    catch {
      case e: Exception => {
        throw new RuntimeException("Error received while sending file", e)
      }
    }
  }


  def sendCommand(cmd: String): Int = {
    var ch: ChannelExec = null
    try {
      logger.debug("Sending command {} to remote machine {}", cmd, session.getHost)
      ch = session.openChannel("exec").asInstanceOf[ChannelExec]
      //ch.setCommand(". /etc/profile ; " + cmd)
      ch.setInputStream(null)
      ch.connect
      readAllStdOutput(ch)
      output.flush
      ch.disconnect
      ch.getExitStatus
    }
    catch {
      case e: JSchException => {
        throw new RuntimeException("Error received while sending command", e)
      }
      case e: IOException => {
        throw new RuntimeException("Error received while sending command", e)
      }
    }
  }

  private def readAllStdOutput(ch: ChannelExec): Unit = {
    var in: InputStream = ch.getInputStream
    var tmp = new Array[Byte](1024)
    var continue = true;
    while (continue) {
      var continue2 = true
      while (in.available > 0 && continue2) {
        var i: Int = in.read(tmp, 0, 1024)
        if (i < 0) {
          continue2 = false
        }
        else {
          output.write(tmp, 0, i)
        }
      }

      if (ch.isClosed) {
        output.flush
        continue = false
      }
      else {
        try {
          Thread.sleep(1000)
        }
        catch {
          case e: Exception => {
            logger.error("error during readAllStdErrOutput", e)
          }
        }
      }
    }

    try {
      Thread.sleep(1000)
    }
    catch {
      case e: InterruptedException => {
        logger.error("error during readAllStdErrOutput", e)
      }
    }
    output.flush

  }


  private def checkAck(in: InputStream): Int = {
    try {
      var b: Int = in.read
      if (b == 0) return b
      if (b == -1) return b
      if (b == 1 || b == 2) {
        var sb: StringBuffer = new StringBuffer
        var c: Int = 0
        do {
          c = in.read
          sb.append(c.asInstanceOf[Char])
        } while (c != '\n')
        if (b == 1) {
          System.out.print(sb.toString)
        }
        if (b == 2) {
          System.out.print(sb.toString)
        }
      }
      return b
    }
    catch {
      case e: Exception => {
        throw new RuntimeException("Error received during checkAck (sending file)", e)
      }
    }
  }


}