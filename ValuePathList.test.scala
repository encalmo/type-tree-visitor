package org.encalmo.utils

class ValuesPathListSpec extends munit.FunSuite {

  import ValuesPathList.compute

  test("compute the list of value paths of a primitive value") {
    assertEquals(compute(1).mkString(","), "")
  }

  test("compute the list of value paths of a case class") {
    case class Example(a: Int, b: String)
    val example1 = Example(a = 1, b = "foo")
    val example2 = Example(a = 2, b = "bar")

    assertEquals(compute(example1).mkString(","), ",a,b")
  }

  test("compute the list of value paths of a complex case class") {
    case class Nested[A](a: A, b: A)
    case class ComplexCaseClass[A](a: Int, b: String, c: List[Int], d: (Int, String, Boolean), e: Nested[A])

    val example1 =
      ComplexCaseClass(a = 1, b = "foo", c = List(1, 2, 3), d = (1, "foo", true), e = Nested(a = 1, b = 2))
    val example2 =
      ComplexCaseClass(a = 2, b = "bar", c = List(4, 5, 6), d = (2, "bar", false), e = Nested(a = 3, b = 4))
    val example3 =
      ComplexCaseClass(a = 1, b = "foo", c = List(1, 2, 3), d = (1, "foo", true), e = Nested(a = "foo", b = "bar"))

    assertEquals(compute(example1).mkString(","), ",a,b,c,c[0],c[1],c[2],d,d[0],d[1],d[2],e,e.a,e.b")
    assertEquals(compute(example2).mkString(","), ",a,b,c,c[0],c[1],c[2],d,d[0],d[1],d[2],e,e.a,e.b")
    assertEquals(compute(example3).mkString(","), ",a,b,c,c[0],c[1],c[2],d,d[0],d[1],d[2],e,e.a,e.b")

  }

}
