package com.lasic.interpreter

import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith
import org.scalatest.FlatSpec
import com.lasic.cloud.mock.MockCloud
import org.scalatest.matchers.ShouldMatchers
import com.lasic.model.{SystemGroup, LasicProgram}

/**
 *
 * @author Brian Pugh
 */

@RunWith(classOf[JUnitRunner])
class RunActionVerbTest extends FlatSpec with ShouldMatchers {
  "RunActionVerb" should "delete the scale group and config" in {
    val cloud = new MockCloud()

    val scalingGroup = cloud.getScalingGroupClient
    scalingGroup.reset
    scalingGroup.createScalingGroup("one", "config", 1, 3, null, null)
    scalingGroup.createScalingGroup("two", "config2", 3, 8, null, null)
    val program = new LasicProgram()
    program.rootGroup = new SystemGroup(program)

    val runAction = new RunActionVerb("test", cloud, program)
    runAction.scaleGroupsToDelete = List("one", "two")
    runAction.scaleGroupsToDelete = List("one", "two")
    runAction.configsToDelete = List("config", "config2")
    runAction.deleteOldScaleGroups

    scalingGroup.getScaleGroups.size should be === 0
    scalingGroup.getlaunchConfigs.size should be === 0
  }
}