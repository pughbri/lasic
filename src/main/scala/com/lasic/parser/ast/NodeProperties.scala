package com.lasic.parser.ast

/**
 * Created by IntelliJ IDEA.
 * User: lmonson
 * Date: May 11, 2010
 * Time: 10:06:53 AM
 * To change this template use File | Settings | File Templates.
 */

trait NodeProperties {
  var name                    = ""
  var count                   = 1
  var machineimage: String    = null
  var kernelid: String        = null
  var ramdiskid: String       = null
  var groups: List[String]    = List()
  var key: String             = null
  var user: String            = null
  var instancetype: String    = null
}


/*
     props {
            count: 1
            machineimage: "machineimage"
            kernelid: "kernelid"
            ramdiskid:        "ramdiskid"
            groups:            "group"
            key:              "key"
            user:             "user"
            instancetype:     "small"
        }
        */