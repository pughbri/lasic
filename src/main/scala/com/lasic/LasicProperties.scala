package com.lasic

/**
 * Created by IntelliJ IDEA.
 * User: pughbc
 * Date: May 12, 2010
 * Time: 1:57:01 PM
 * To change this template use File | Settings | File Templates.
 */

//TODO: is there a scala thing out there like groovy ConfigurationHolder
object LasicProperties {

  /** The name of the properties file  **/
   //TODO: Should be loaded from the lasic script like old lasic did
  private val propFilename = "/lasic.properties"

  /**The loaded properties */
  private val props = {
    val props = new java.util.Properties
    val stream = classOf[Application].getResourceAsStream(propFilename)
    if (stream != null)
      props.load(stream)
    props
  }

  def getProperty(key: String): String = {
    //todo: look for system property overrides first
    props.getProperty(key)
  }

  def getProperty(key: String, defaultValue: String): String = {
    props.getProperty(key, defaultValue)
  }

  def resolveProperty(text: String): String = {
    //quick hackjob to replace vars.  This should be handled by framework if we decide to use configgy or something
    var beginVarIndex = text.indexOf("${", 0)
    var newText = "";
    var endVarIndex = 0
    while (beginVarIndex != -1) {
      newText += text.substring(endVarIndex, beginVarIndex)
      endVarIndex = text.indexOf("}",beginVarIndex)
      newText += getProperty(text.substring(beginVarIndex + 2, endVarIndex))
      beginVarIndex = text.indexOf("${", endVarIndex)
      endVarIndex += 1
    }
    newText
  }
}