package com.lasic.parser.ast

import com.lasic.model.ScriptArgumentValue

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
  var scpMap                  = Map[String,String]()
  var scriptMap               = Map[String,Map[String,ScriptArgumentValue]]()
  var volumes                 = List[Map[String,String]]()

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