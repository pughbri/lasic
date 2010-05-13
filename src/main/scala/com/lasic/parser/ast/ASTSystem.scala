package com.lasic.parser.ast


/**
 * Created by IntelliJ IDEA.
 * User: lmonson
 * Date: May 10, 2010
 * Time: 2:29:49 PM
 * To change this template use File | Settings | File Templates.
 */

import com.lasic.model.{SystemInstance, SystemProperties, SystemGroup}
import collection.mutable.{ListBuffer, HashMap}


class ASTSystem extends SystemProperties {
  val nodes = new ListBuffer[ASTNode]()
  val subsystems = new ListBuffer[ASTSystem]()
}




/*
 node "a node" {
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

		scripts {
			"some_script": {}
			"another": {
				foo:"bar"
			}
		}

        scp {
			"src":"dest"
			"src2":"dest2"
        }
*/