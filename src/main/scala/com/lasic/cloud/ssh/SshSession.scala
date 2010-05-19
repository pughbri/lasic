package com.lasic.cloud.ssh

import com.jcraft.jsch._
import java.io._

/**
 *
 * User: Brian Pugh
 * Date: May 13, 2010
 */

class SshSession extends JSch {
  private var isConnected: Boolean = false

  //TODO: something real with output handling
  private val output: OutputStream = System.out

  private var session: Session = null


  def connect(dnsName: String, userName: String, pemFile: File): Unit = {
    if (isConnected) {
      //logger.error("Attempted to connect to {} but this SSH session is already in use", dnsName)
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
        new ConnectException(e)
      }
    }
  }


  def disconnect: Unit = {
    try {
      removeAllIdentity
      if (session != null) {
        session.disconnect
      }
    }
    catch {
      case e: Exception => {
        //logger.error("Error received trying to disconnect", e)
        throw new RuntimeException(e)
      }
    }
    isConnected = false
  }


  def sendFile(f: File, remoteFileName: String): Int = {
    try {
      //logger.debug("Sending local file {} to remote machine as {}", f.getCanonicalFile, remoteFileName)
      var fis: FileInputStream = null
      var command: String = "scp -p -t " + remoteFileName
      var channel: Channel = session.openChannel("exec")
      (channel.asInstanceOf[ChannelExec]).setCommand(command)
      var out: OutputStream = channel.getOutputStream
      var in: InputStream = channel.getInputStream
      channel.connect
      if (checkAck(in) != 0) {
        //        logger.warn("checkAck returned non-zero")
        throw new RuntimeException("Send file failed")
      }
      var filesize: Long = f.length
      command = "C0644 " + filesize + " " + f.getName
      command += "\n"
      out.write(command.getBytes)
      out.flush
      if (checkAck(in) != 0) {
        throw new RuntimeException
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
        //logger.error("Error received while sending file", e)
        throw new RuntimeException(e)
      }
    }
  }


  def sendCommand(cmd: String): Int = {
    var ch: ChannelExec = null
    try {
      ch = session.openChannel("exec").asInstanceOf[ChannelExec]
      ch.setCommand(". /etc/profile ; " + cmd)
      ch.setInputStream(null)
      ch.connect
      readAllStdOutput(ch)
      output.flush
      ch.disconnect
      ch.getExitStatus
    }
    catch {
      case e: JSchException => {
        // logger.error("Error received while sending command", e)
        throw new RuntimeException(e)
      }
      case e: IOException => {
        //logger.error("Error received while sending command", e)
        e.printStackTrace
        throw new RuntimeException(e)
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
            println("error during readAllStdErrOutput ")
            e.printStackTrace
          }
        }
      }
    }

    try {
      Thread.sleep(1000)
    }
    catch {
      case e: InterruptedException => {
        e.printStackTrace
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
        //        logger.error("Error received during checkAck (sending file)", e)
        throw new RuntimeException(e)
      }
    }
  }


}