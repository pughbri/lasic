package com.lasic.cloud

import java.util.Calendar

/**
 *
 * User: Brian Pugh
 * Date: May 20, 2010
 */

class VolumeInfo(val volumeId: String,
                 val size: String,
                 val snapshotId: String,
                 val zone: String,
                 val status: String,
                 val createTime: Calendar,
                 val attachementSets: List[AttachmentInfo]) {
}