package com.lasic.parser.ast

import com.lasic.values.{ScaleGroupProperties, NodeProperties}

/**
 * 
 * @author Brian Pugh
 */
class ASTScaleGroup extends NodeProperties  with ScaleGroupProperties {
  var triggers: List[ASTTrigger]    = List()
}