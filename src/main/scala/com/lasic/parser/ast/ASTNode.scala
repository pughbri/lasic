package com.lasic.parser.ast

import com.lasic.values.{BaseAction, NodeProperties}
import com.lasic.model.ArgumentValue

/**
 * Created by IntelliJ IDEA.
 * User: lmonson
 * Date: May 11, 2010
 * Time: 10:04:02 AM
 * To change this template use File | Settings | File Templates.
 */


class ASTNode(val isAbstract: Boolean = false, val parentNode: Option[String]) extends NodeProperties {
  var volumes                 = List[Map[String,String]]()
  var actions                 = List[BaseAction]()
  var loadBalancers           = List[ArgumentValue]()
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