package com.lasic.cloud

import java.util.Calendar

/**
 *
 * User: Brian Pugh
 * Date: May 20, 2010
 */

class AttachmentInfo(val volumeId: String,
                     val instanceId: String,
                     val device: String,
                     val status: String,
                     val attachTime: Calendar) {
  override def toString = "volumeId[" + volumeId + "] instanceId[" + instanceId + "] device[" + device +
          "] status[" + status + "] attachTime[" + attachTime + "]"
}