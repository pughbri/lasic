object MyCounter {
  var system = 1;
  var node = 1

  def nextSystem = {
    val x = system
    system = system + 1
    "System%03d".format( x )
  }
  def nextNode = {
    val x = node
    node = node + 1
    "Node%03d".format( x )
  }
}