package com.lasic.model

import com.lasic.cloud.VM
import com.lasic.cloud.MachineState

/**
 *
 * Holds a VM and delegates to it for the calls commonly needed by Verbs.
 * @author Brian Pugh
 */

trait VMHolder {
  var vm: VM = null

  def vmId = {
    if (vm != null) vm.instanceId else "?"
  }

  def vmState = {
    if (vm != null) vm.getMachineState else MachineState.Unknown
  }

  def vmPublicDns = {
    if (vm != null) vm.getPublicDns else "?"
  }

  def vmPrivateDns = {
    if (vm != null) vm.getPrivateDns else "?"
  }

}