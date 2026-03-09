package org.encalmo.utils

class InternalStructureHashcodeSpec extends munit.FunSuite {

  import InternalStructureHashcode.compute

  test("compute the internal structure hashcode of a primitive value") {
    val hashcodeInt = compute[Int]
    val hashcodeDouble = compute[Double]
    val hashcodeLong = compute[Long]

    assertEquals(hashcodeInt, hashcodeDouble)
    assertEquals(hashcodeInt, hashcodeLong)
  }

  test("compute the internal structure hashcode of a case class") {
    case class Example1(a: Int, b: String)
    case class Example2(a: Int, b: String)
    case class Example3(a: Int, b: Boolean)

    assertEquals(compute[Example1], compute[Example2])
    assertNotEquals(compute[Example1], compute[Example3])
    assertNotEquals(compute[Example3], compute[Example2])
  }

  test("compute the internal structure hashcode of a complex case class") {
    case class Nested[A](a: A, b: A)
    case class ComplexCaseClass1[A](a: Int, b: String, c: List[Int], d: (Int, String, Boolean), e: Nested[A])
    case class ComplexCaseClass2[A](a: Int, b: String, c: List[Int], d: (Int, String, Boolean), e: Nested[A])

    assertEquals(compute[ComplexCaseClass1[Int]], compute[ComplexCaseClass2[Int]])
  }

  test("compute the internal structure hashcode of a Person case class") {
    assertEquals(compute[Person], compute[Person])
  }

}
