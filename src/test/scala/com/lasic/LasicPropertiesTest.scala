package com.lasic

import junit.framework.TestCase
import java.io.File
import java.lang.String

/**
 * Created by IntelliJ IDEA.
 * User: pughbc
 * Date: May 12, 2010
 * Time: 2:02:58 PM
 * To change this template use File | Settings | File Templates.
 */

class LasicPropertiesTest extends TestCase("MockCloudTest") {
  override def setUp = {
    System.setProperty("properties.file", new File(classOf[Application].getResource("/lasic.properties").toURI()).getCanonicalPath())
  }

  def testGetProperty() = {
    val prop1: String = LasicProperties.getProperty("ACCESS_KEY")
    assert("access_key1" == prop1, "expected access_key1 got " + prop1)
    val prop2: String = LasicProperties.getProperty("SECRET_KEY")
    assert("secret_key1" == prop2, "expected secret_key1 got " + prop2)
    val prop3: String = LasicProperties.getProperty("unknown_key")
    assert(null == prop3, "expected secret_key1 got " + prop3)
  }

  def testGetPropertyWithDefault() = {
    val property: String = LasicProperties.getProperty("unknown_key", "junk")
    assert("junk" == property, "expected junk got " + property)
  }

  def testGetSystemProperty() = {
    val key: String = "___A_VERY_OBSCURE_KEY___"
    val value: String = "new_val"
    System.setProperty(key, value)
    val prop1: String = LasicProperties.getProperty(key)
    assert(value == prop1, "expected " + value + " got " + prop1)

  }

  def testSetPropertyFileName() = {
    //this test depends on the system property being set BEFORE the LasicProperties is initialized
    //which won't be true all the time (when running from mvn or ide that runs more than 1 test)
    if (false) {
      System.setProperty("properties.file", "/lasic2.properties")
      val prop1: String = LasicProperties.getProperty("ACCESS_KEY")
      assert("access_key2" == prop1, "expected access_key2 got " + prop1)
    }

  }


  def testResolveProperty() = {
    val property: String = LasicProperties.resolveProperty("${this} string${unknown_variable_should_dissappear} has ${two} variables in ${it}")
    assert(property == "my string has 2 variables in itself", "got: " + property)
  }

  def testQuotedString() = {
    val property = LasicProperties.resolveProperty("\"var${it}\"");
    assert( property=="\"varitself\"", "got: "+property)
  }
  def testNoProperties() = {
    val property: String = LasicProperties.resolveProperty("this string has no properties")
    assert (property == "this string has no properties", "got: " + property)

  }
}