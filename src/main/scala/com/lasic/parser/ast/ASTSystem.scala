package com.lasic.parser.ast


/**
 * Created by IntelliJ IDEA.
 * User: lmonson
 * Date: May 10, 2010
 * Time: 2:29:49 PM
 * To change this template use File | Settings | File Templates.
 */

import scala.collection.mutable.Map

class ASTSystem extends SystemProperties {
  val nodes:Map[String,ASTNode] = scala.collection.mutable.Map()
  val subsystems:Map[String,ASTSystem] = scala.collection.mutable.Map()

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