package com.lasic.cloud


/**
 *
 * User: Brian Pugh
 * Date: May 14, 2010
 */

class MachineDescription(val instanceId: String,
                         var state: MachineState.Value,
                         val publicDNS: String,
                         val privateDNS: String) {

}