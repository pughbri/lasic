package com.lasic.values

import com.lasic.model.{Action, ScriptArgumentValue}

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