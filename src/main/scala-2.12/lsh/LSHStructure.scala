package lsh

import messages.{InitRepetition, Query}

import scala.concurrent.{Await, Future}
import akka.actor._
import akka.util.Timeout

import scala.concurrent.duration._
import akka.pattern.ask
import hashfunctions.HashFunction
import measures.Distance

import scala.collection.mutable.ArrayBuffer

/**
  * Structure to be queried on for
  * approximate nearest neighbors
  * stored in its internal hashmaps
  */

class LSHStructure(repetitions:Array[ActorSelection]) {

  /**
    * When Initializing LSH with a set of repetitions
    * Each repetition is reset, and rebuilt
    */

  // TODO Change content in messages between nodes to be simple arrays instead of objects
  val resultSets:ArrayBuffer[Future[Any]] = new ArrayBuffer(repetitions.length) // TODO Cannot use array in .sequence method, ... consider another method.
  val statuses:ArrayBuffer[Future[Any]] = new ArrayBuffer(repetitions.length)
  implicit val timeout = Timeout(10.hours)

  import scala.concurrent.ExecutionContext.Implicits.global

  def query(qp:Array[Float], k:Int) : ArrayBuffer[Int] = {
    // for each rep, send query, wait for result from all. return set
    var i = 0
    while(i < repetitions.length) {
      resultSets += repetitions(i) ? Query(qp, k)
      i += 1
    }

    // Wait for all results to return
    // TODO Future sequence is a linear cost
    val res = Await.result(Future.sequence(resultSets), timeout.duration).asInstanceOf[ArrayBuffer[ArrayBuffer[(Int, Double)]]]

    res.flatten.sortBy(x => x._2).take(k).map(x => x._1)
  }

  def build(filePath:String, hashFunction:String, functions:Int, dimensions:Int, simMeasure:Distance, seed:Long) : Boolean = {
    var i = 0
    while(i < repetitions.length) {
      statuses += repetitions(i) ? InitRepetition(filePath, hashFunction, functions, dimensions, simMeasure, seed)
      i += 1
    }

    // waiting for all tables to finish
    // TODO if all is successful, then return
    val res = Await.result(Future.sequence(statuses), timeout.duration).asInstanceOf[ArrayBuffer[Boolean]]
    res.forall(x => x)
  }
}
