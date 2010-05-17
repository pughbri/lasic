package com.lasic.cloud.ssh

import java.io.{File, OutputStream}
import com.jcraft.jsch._

/**
 * 
 * User: Brian Pugh
 * Date: May 13, 2010
 */

class SshSession extends JSch {
  private var isConnected: Boolean = false
  private val output: OutputStream = null
  private var dnsName: String = null
  private var userName: String = null
  private var session: Session = null
  private var ch: ChannelExec = null


  def connect(dnsName: String, userName: String, pemFile: File): Boolean = {
    if (isConnected) {
      //logger.error("Attempted to connect to {} but this SSH session is already in use", dnsName)
      throw new RuntimeException("Attempted to connect to " + dnsName + " but this SSH session is already in use")
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
      this.dnsName = dnsName
      this.userName = userName
      return true
    }
    catch {
      case e: JSchException => {
        isConnected = false
        //logger.debug("Can't create SSH connection with {}", dnsName)
        return false
      }
    }
  }


  def disconnect: Unit = {
    try {
      dnsName = null
      userName = null
      removeAllIdentity
      session.disconnect
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
        1
//        logger.debug("Sending local file {} to remote machine as {}", f.getCanonicalFile, remoteFileName)
//        var fis: FileInputStream = null
//        var command: String = "scp -p -t " + remoteFileName
//        var channel: Channel = session.openChannel("exec")
//        (channel.asInstanceOf[ChannelExec]).setCommand(command)
//        var out: OutputStream = channel.getOutputStream
//        var in: InputStream = channel.getInputStream
//        channel.connect
//        if (checkAck(in) != 0) {
//          logger.warn("checkAck returned non-zero")
//          throw new RuntimeException("Send file failed")
//        }
//        var filesize: Long = f.length
//        command = "C0644 " + filesize + " " + f.getName
//        command += "\n"
//        out.write(command.getBytes)
//        out.flush
//        if (checkAck(in) != 0) {
//          throw new RuntimeException
//        }
//        fis = new FileInputStream(f)
//        var buf: Nothing = new Nothing(1024)
//        while (true) {
//          var len: Int = fis.read(buf, 0, buf.length)
//          if (len <= 0) break //todo: break is not supported
//          out.write(buf, 0, len)
//        }
//        fis.close
//        fis = null
//        buf(0) = 0
//        out.write(buf, 0, 1)
//        out.flush
//        if (checkAck(in) != 0) {
//          System.exit(0)
//        }
//        out.close
//        channel.disconnect
//        return channel.getExitStatus
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
        1
//        ch = session.openChannel("exec").asInstanceOf[ChannelExec]
//        ch.setCommand(". /etc/profile ; " + cmd)
//        ch.setInputStream(null)
//        ch.connect
//        readAllStdOutput(ch)
//        output.flush
//        ch.disconnect
//        return ch.getExitStatus
      }
      catch {
        case e: JSchException => {
         // logger.error("Error received while sending command", e)
          throw new RuntimeException(e)
        }
//        case e: IOException => {
//          logger.error("Error received while sending command", e)
//          throw new RuntimeException(e)
//        }
      }
    }


}