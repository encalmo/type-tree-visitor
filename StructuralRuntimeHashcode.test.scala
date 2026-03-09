package org.encalmo.utils

import java.time.LocalDate

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

  test("compute the structural hashcode of a Person case class") {
    val person1 = Person(
      id = "1234567890",
      name = "John Doe",
      age = 30,
      stature = 1.80,
      email = Some("john.doe@example.com"),
      address = Some(Address(street = "123 Main St", city = "Anytown", postcode = "12345")),
      addresses = Some(Map("home" -> Address(street = "123 Main St", city = "Anytown", postcode = "12345"))),
      isStudent = false,
      tags = List(tag(name = "tag1", value = "value1")),
      citizenship = Citizenship.UK,
      immigrationStatus = Some(new ImmigrationStatus {
        def status: String = "valid"
        def validUntil: LocalDate = LocalDate.now()
      }),
      maritalStatus = Array(MaritalStatus.Single),
      hobbies = List(Hobby.Reading),
      hobby = Hobby.Reading,
      passportNumber = Some(PassportNumber("1234567890")),
      driverLicense = Some(DriverLicense(number = "1234567890", expiryDate = LocalDate.now())),
      disabilities = List(Disability(value = "disability1")),
      disability = Disability(value = "disability1"),
      benefits1 = List(Benefit.ChildBenefit),
      benefits2 = List(Benefit.ChildBenefit),
      skills = Skills("skill1", "skill2"),
      wallet = (1, "wallet1", LocalDate.now()),
      assets = (Cars.Ford, Boats("boat1"), Boeing("Boeing 747")),
      books = List(("author1", "title1"), ("author2", "title2")),
      bookAtDesk = ("author3", "title3"),
      hand1 = Left("hand1"),
      hand2 = Right("hand2"),
      status = Status.ACTIVE,
      active = YesNo(true)
    )
    compute(person1)
  }

}
