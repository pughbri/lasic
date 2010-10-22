package com.lasic.parser.ast

import com.lasic.values.{BaseAction, ScaleGroupProperties, NodeProperties}
import com.lasic.model.ArgumentValue

/**
 * 
 * @author Brian Pugh
 */
class ASTScaleGroup {
  var name = ""
  var configuration: ASTScaleGroupConfig = null
  var triggers: List[ASTTrigger]    = List()
  var volumes                 = List[Map[String,String]]()
  var actions                 = List[BaseAction]()
  var loadBalancers           = List[ArgumentValue]()
}