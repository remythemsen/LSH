package hashfunctions

trait HashFunction {
  def apply(v: Array[Float]): Array[Int]
  def generateProbes(v : Array[Float]): Array[Array[Int]]
}



