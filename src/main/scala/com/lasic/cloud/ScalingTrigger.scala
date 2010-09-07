package com.lasic.cloud

class ScalingTrigger(
        var autoScalingGroupName: String,
        var breachDuration: Int,
        var lowerBreachScaleIncrement: String,
        var lowerThreshold: Double,
        var measureName: String,
        var name: String,
        var namespace: String,
        var period: Int,
        var upperBreachScaleIncrement: String,
        var upperThreshold: Double
        )