package com.lasic.parser.ast

import scala.collection.mutable.Map

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
  var scp                     = new scala.collection.mutable.HashMap()

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