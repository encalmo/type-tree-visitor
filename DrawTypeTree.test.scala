package org.encalmo.utils

class DrawTypeTreeSpec extends munit.FunSuite {

  import DrawTypeTree.draw

  test("draw type tree of a primitive value") {
    assertEquals(draw[Int], "Int")
  }

  test("draw type tree of a case class") {
    case class Example(a: Int, b: String)
    assertEquals(
      draw[Example],
      """|Example
         |    ├── a: Int
         |    └── b: String""".stripMargin
    )
  }

  test("draw type tree of a Person case class") {
    assertEquals(
      draw[Person],
      """|Person
         |    ├── id: String
         |    ├── name: Name
         |    ├── age: Int
         |    ├── stature: Double
         |    ├── email: Option
         |    │   └── String
         |    │
         |    ├── address: Option
         |    │   └── Address
         |    │       ├── street: String
         |    │       ├── city: String
         |    │       └── postcode: String
         |    │
         |    ├── addresses: Option
         |    │   └── Map
         |    │       └── <key>: Address
         |    │           ├── street: String
         |    │           ├── city: String
         |    │           └── postcode: String
         |    │
         |    ├── isStudent: Boolean
         |    ├── tags: List
         |    │   └── tag
         |    │       ├── name: String
         |    │       └── value: String
         |    │
         |    ├── citizenship: Citizenship
         |    ├── immigrationStatus: Option
         |    │   └── ImmigrationStatus
         |    │
         |    ├── maritalStatus: Array
         |    │   └── MaritalStatus
         |    │       ├── CivilPartnership
         |    │       │   ├── partnerName: String
         |    │       │   └── from: LocalDate
         |    │       │
         |    │       ├── Married
         |    │       │   ├── partnerName: String
         |    │       │   └── from: LocalDate
         |    │       │
         |    │       ├── Divorced
         |    │       │   └── from: LocalDate
         |    │       │
         |    │       └── Widowed
         |    │           └── from: LocalDate
         |    │
         |    ├── hobbies: List
         |    │   └── Hobby
         |    │       └── Other
         |    │           └── name: String
         |    │
         |    ├── hobby: Hobby
         |    │   └── Other
         |    │       └── name: String
         |    │
         |    ├── passportNumber: Option
         |    │   └── PassportNumber
         |    │
         |    ├── driverLicense: Option
         |    │   └── DriverLicense
         |    │       └── Document
         |    │           ├── number: String
         |    │           └── expiryDate: LocalDate
         |    │
         |    ├── disabilities: List
         |    │   └── Disability
         |    │
         |    ├── disability: Disability
         |    ├── benefits1: List
         |    │   ├── List
         |    │   │   └── Benefit
         |    │   │       └── Other
         |    │   │           └── name: String
         |    │   │
         |    │   └── false
         |    │
         |    ├── benefits2: List
         |    │   ├── List
         |    │   │   └── Benefit
         |    │   │       └── Other
         |    │   │           └── name: String
         |    │   │
         |    │   └── false
         |    │
         |    ├── skills: Skills
         |    │   └── String
         |    │
         |    ├── wallet: Tuple3
         |    │   ├── Int
         |    │   ├── String
         |    │   └── LocalDate
         |    │
         |    ├── assets: Tuple3
         |    │   ├── Cars
         |    │   │   └── Other
         |    │   │       └── name: String
         |    │   │
         |    │   ├── Boats
         |    │   │   └── String
         |    │   │
         |    │   └── Planes
         |    │       ├── Boeing
         |    │       │   └── model: String
         |    │       │
         |    │       └── Airbus
         |    │           └── model: String
         |    │
         |    ├── books: List
         |    │   └── NamedTuple
         |    │       ├── author: String
         |    │       └── title: String
         |    │
         |    ├── bookAtDesk: NamedTuple
         |    │   ├── author: String
         |    │   └── title: String
         |    │
         |    ├── hand1: Either
         |    │   ├── String
         |    │   └── String
         |    │
         |    ├── hand2: Either
         |    │   ├── String
         |    │   └── String
         |    │
         |    ├── status: Status
         |    └── active: YesNo""".stripMargin
    )
  }

}
