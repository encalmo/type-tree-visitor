package org.encalmo.utils

/** A hashcode is a 32-bit integer used to uniquely identify an object during program execution. */
class Hashcode {

  val prime = 31
  var hashcode: Int = 17

  def update(any: Any): Unit = {
    hashcode = hashcode * prime + any.hashCode
  }

  def result: Int = hashcode

}
