package com.lasic

import java.lang.String
import util.Logging
import java.io.{FileNotFoundException, FileInputStream}

/**
 * User: pughbc
 * Date: May 12, 2010
 */
//TODO: is there a scala thing out there like groovy ConfigurationHolder
object LasicProperties extends Logging {

  /**The name of the System properties that specifies the name of the properties file**/
  val SYSTEM_PROPERTY_FOR_FILENAME = "properties.file"


  private[this] var propFilenameInternal = {
    val propFile: String = System.getProperty(SYSTEM_PROPERTY_FOR_FILENAME)
    if (propFile != null) propFile else System.getProperty("user.home") + "/.lasic/lasic.properties"
  }

  /**The name of the properties file  **/
  def propFilename = propFilenameInternal

  def propFilename_=(newName: String) {
    propFilenameInternal = newName
    props = createNewProperties()
  }


  /**The loaded properties */
  private var props = createNewProperties()

  private def createNewProperties(): java.util.Properties = {
    val props = new java.util.Properties
    var inputStream: FileInputStream = null
    try {
      inputStream = new FileInputStream(propFilenameInternal)
      props.clear
      props.load(inputStream)
      logger.debug("Loaded properties from [{}]. Properties: {}", propFilenameInternal, props)
    }
    catch {
      case e: FileNotFoundException => logger.warn("unable to find lasic properties file at ["
              + propFilenameInternal + "].  No properties will be loaded")
    }
    finally {
      if (inputStream != null) inputStream.close
    }
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