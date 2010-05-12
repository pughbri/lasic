package com.lasic

import junit.framework.TestCase
import java.lang.String

/**
 * Created by IntelliJ IDEA.
 * User: pughbc
 * Date: May 12, 2010
 * Time: 2:02:58 PM
 * To change this template use File | Settings | File Templates.
 */

class LasicPropertiesTest extends TestCase("MockCloudTest") {
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


  def testResolveProperty() = {
    val property: String = LasicProperties.resolveProperty("${this} string has ${two} variables in ${it}")
    assert (property == "my string has 2 variables in itself", "got: " + property)
  }
}