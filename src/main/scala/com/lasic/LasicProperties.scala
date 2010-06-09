package com.lasic

import java.lang.String
import util.Logging

/**
 * User: pughbc
 * Date: May 12, 2010
 */
//TODO: is there a scala thing out there like groovy ConfigurationHolder
object LasicProperties extends Logging {

    /** The name of the System properties that specifies the name of the properties file**/
  val SYSTEM_PROPERTY_FOR_FILENAME = "properties.file"

  /** The name of the properties file  **/
  private val propFilename = {
    val propFile: String = System.getProperty(SYSTEM_PROPERTY_FOR_FILENAME)
    if (propFile != null) propFile else "/lasic.properties"
  }

  /**The loaded properties */
  private val props = {
    val props = new java.util.Properties
    val stream = classOf[Application].getResourceAsStream(propFilename)
    if (stream != null)
      props.load(stream)
    props
  }

  def getProperty(key: String): String = {
    getProperty(key, null)
  }

  def getProperty(key: String, defaultValue: String): String = {
    val sysProperty: String = System.getProperty(key)
    if (sysProperty != null) sysProperty else props.getProperty(key, defaultValue)
  }

  def resolveProperty(text: String): String = {
    val regex = """\$\{[^\}]*\}""".r
    var result = text

    for(x:String <- regex findAllIn text) {
      val target = x.substring(2,x.length-1)
      var propValue = getProperty(target)
      if (propValue == null) {
        logger.warn("property " + target + " is not set")
        propValue = ""
      }

      result = result.replaceAll("\\$\\{"+target+"\\}",propValue)
    }
    result;

  }
}