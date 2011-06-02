package com.lasic

import java.lang.String
import org.apache.commons.configuration.PropertiesConfiguration
import util.Logging

/**
 * User: pughbc
 * Date: May 12, 2010
 */
//TODO: is there a scala thing out there like groovy ConfigurationHolder
object LasicProperties extends Logging {

  /**The name of the System properties that specifies the name of the properties file**/
  val SYSTEM_PROPERTY_FOR_FILENAME = "properties.file"

  private var env: String = null

  lazy val config = {
    new PropertiesConfiguration(if (env != null) {
      System.getProperty("user.home")+"/.lasic/"+env+".properties"
    } else {  
      propFilenameInternal
    })
  }

  def setEnv(env: String) = {
    this.env = env
  }
  
  private[this] var propFilenameInternal = {
    val propFile: String = System.getProperty(SYSTEM_PROPERTY_FOR_FILENAME)
    if (propFile != null) propFile else System.getProperty("user.home") + "/.lasic/lasic.properties"
  }

  def getProperty(key: String): String = {
    getProperty(key, null)
  }

  def getProperty(key: String, defaultValue: String): String = {
    val sysProperty: String = System.getProperty(key)
    if (sysProperty != null) sysProperty else config.getString(key, defaultValue)
  }

  def resolveProperty(text: String): String = {
    val regex = """\$\{[^\}]*\}""".r
    var result = text

    for (x: String <- regex findAllIn text) {
      val target = x.substring(2, x.length - 1)
      var propValue = getProperty(target)
      if (propValue == null) {
        logger.warn("property " + target + " is not set")
        propValue = ""
      }

      result = result.replaceAll("\\$\\{" + target + "\\}", propValue)
    }
    result;

  }
}