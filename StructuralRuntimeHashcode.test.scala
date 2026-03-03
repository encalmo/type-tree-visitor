package org.encalmo.utils

class StructuralRuntimeHashcodeSpec extends munit.FunSuite {

  import StructuralRuntimeHashcode.compute

  test("compute the structural hashcode of a primitive value") {
    val hashcodeInt1 = compute(1)
    val hashcodeInt2 = compute(2)
    val hashcodeDouble1 = compute(1.0)
    val hashcodeLong1 = compute(1L)

    assertEquals(hashcodeInt1, hashcodeInt2)
    assert(hashcodeInt1 != hashcodeDouble1)
    assert(hashcodeInt1 != hashcodeLong1)
  }

  test("compute the structural hashcode of a case class") {
    case class Example(a: Int, b: String)
    val example1 = Example(a = 1, b = "foo")
    val example2 = Example(a = 2, b = "bar")

    assertEquals(compute(example1), compute(example2))
  }

  test("compute the structural hashcode of a complex case class") {
    case class Nested[A](a: A, b: A)
    case class ComplexCaseClass[A](a: Int, b: String, c: List[Int], d: (Int, String, Boolean), e: Nested[A])

    val example1 =
      ComplexCaseClass(a = 1, b = "foo", c = List(1, 2, 3), d = (1, "foo", true), e = Nested(a = 1, b = 2))
    val example2 =
      ComplexCaseClass(a = 2, b = "bar", c = List(4, 5, 6), d = (2, "bar", false), e = Nested(a = 3, b = 4))
    val example3 =
      ComplexCaseClass(a = 1, b = "foo", c = List(1, 2, 3), d = (1, "foo", true), e = Nested(a = "foo", b = "bar"))

    assertEquals(compute(example1), compute(example2))
    assertNotEquals(compute(example1), compute(example3))
    assertNotEquals(compute(example2), compute(example3))
  }

}
