<a href="https://github.com/encalmo/type-tree-visitor">![GitHub](https://img.shields.io/badge/github-%23121011.svg?style=for-the-badge&logo=github&logoColor=white)</a> <a href="https://central.sonatype.com/artifact/org.encalmo/type-tree-visitor_3" target="_blank">![Maven Central Version](https://img.shields.io/maven-central/v/org.encalmo/type-tree-visitor_3?style=for-the-badge)</a> <a href="https://encalmo.github.io/type-tree-visitor/scaladoc/org/encalmo/utils.html" target="_blank"><img alt="Scaladoc" src="https://img.shields.io/badge/docs-scaladoc-red?style=for-the-badge"></a>

# type-tree-visitor

## Table of contents

- [Motivation](#motivation)
- [Dependencies](#dependencies)
- [Usage](#usage)
- [How to start?](#how-to-start?)
- [Term-less iterator and visitor](#term-less-iterator-and-visitor)
- [How to work with typeclasses?](#how-to-work-with-typeclasses?)
   - [Summoning existing typeclasses](#summoning-existing-typeclasses)
   - [Deriving typeclass instances autonomously using macro](#deriving-typeclass-instances-autonomously-using-macro)
   - [Providing typeclass instances semi-autonomously](#providing-typeclass-instances-semi-autonomously)
   - [Preventing circular derivation calls](#preventing-circular-derivation-calls)
- [Examples of macro](#examples-of-macro)
   - [[XmlWriter](https://github.com/encalmo/xmlwriter)](#[xmlwriter](https://github.com/encalmo/xmlwriter))
   - [[StructuralRuntimeHashcode](StructuralRuntimeHashcode.scala)](#[structuralruntimehashcode](structuralruntimehashcode.scala))
   - [[ValuePathsList](ValuePathsList.scala)](#[valuepathslist](valuepathslist.scala))
   - [[InternalStructureHashcode](InternalStructureHashcode.scala)](#[internalstructurehashcode](internalstructurehashcode.scala))
   - [[DrawTypeTree](DrawTypeTree.scala)](#[drawtypetree](drawtypetree.scala))
- [Project content](#project-content)

## Motivation

Writing Scala 3 macros that derive code from type structures is genuinely hard. Whether you're using inline/mirrors, quotes/splices, or rolling something custom, you end up re-implementing the same traversal logic over and over — handling case classes, sealed traits, enums, tuples, named tuples, Java records, opaque types, collections... it's a lot of boilerplate before you even get to the interesting part of your macro.

What this library gives you are two core building blocks:

- `TypeTreeIterator` — a fully implemented iterator that walks a type tree recursively, with support for a very wide set of types out of the box (Scala case classes, sealed traits, collections, arrays, enums, tuples, named tuples, selectables, opaque types, primitives, Java enums, records, maps, iterables, and more)
- `TypeTreeVisitor` — an open trait you implement to do the actual code-generation work at each node

The pattern is the classic Visitor pattern, which means the iterator logic and your derivation logic are cleanly separated. You focus on what to do at each type node — the library handles how to get there.
It also ships with a `TypeTreeTermlessIterator` / `TypeTreeTermlessVisitor` pair for cases where you don't need access to the actual runtime value (faster + simpler).

The pattern implemented here has been developed for and later extracted from the [xmlwriter](https://github.com/encalmo/xmlwriter) macro codebase. While there are multiple established ways of implementing macros using inline/mirrors, or quotes/splices, or [hearth](https://github.com/MateuszKubuszok/hearth) library, there can be multiple reasons to use this toolset instead:

- familiar [visitor pattern](https://refactoring.guru/design-patterns/visitor)
- full support for manual, autonomous and semi-autonomous typeclass derivation modes
- algorithm split into fully implemented `iterator` object and open to extension `visitor` trait
- handy `StatementsCache` abstraction from [macro-utils](https://github.com/encalmo/macro-utils) instead of a bit problematic `Quotes`, with built-in support for nested scopes, reference and symbol cache, plus wrapping of a recurrent code in chunked methods on demand
- good coverage of popular and new types:
   - Scala case classes, sealed traits, collections, arrays, enums, tuples, named tuples, selectables, named types, opaque types, and primitives
   - Java enums, records, maps, iterables, and primitives
- easy access to annotations defined on types, values and fields
- support for a custom type context instance carried over type tree iteration
- all (rechargable) batteries included

## Dependencies

   - [Scala](https://www.scala-lang.org) >= 3.7.4
   - org.encalmo [**macro-utils** 0.21.0](https://central.sonatype.com/artifact/org.encalmo/macro-utils_3)

## Usage

Use with SBT

    libraryDependencies += "org.encalmo" %% "type-tree-visitor" % "0.10.0"

or with SCALA-CLI

    //> using dep org.encalmo::type-tree-visitor:0.10.0

## How to start?

An example of a macro using `TypeTreeIterator` together with some `MyMacroVisitor` implementation:

```scala
package mymacro

import scala.quoted.*

object MyMacro {

  private inline def debugMode = true

  inline def myMethod[T](value: T): Int = {
    ${ myMethodImpl[T]('{ value }) }
  }

  private def myMethodImpl[T: Type](expr: Expr[T])(using Quotes): Expr[Int] = {
    import cache.quotes.reflect.*
    given cache: StatementsCache = new StatementsCache
    myMethodUsingTypeTreeIterator(using cache)(TypeRepr.of[T], expr.asTerm)
    cache.asExprOf[Int]
  }

  private def myMethodUsingTypeTreeIterator(using
      cache: StatementsCache
  )(
      tpe: cache.quotes.reflect.TypeRepr,
      valueTerm: cache.quotes.reflect.Term
  ): Unit = {
    val context = new MyMacroContext
    val trace = scala.collection.mutable.Buffer.empty[String]

    context.initialize(using cache)
    TypeTreeIterator.visitNode(using cache, MyMacroVisitor)(
      tpe = tpe,
      valueTerm = valueTerm,
      context = context,
      isCollectionItem = false,
      annotations = Set.empty,
      trace = trace,
      debugIndent = if debugMode then 0 else Int.MinValue,
      summonTypeclassInstance = false // to prevent circular calls from derivation logic 
                                      // the top call must not summon the typeclass
    )
    context.finalize(using cache)

    import cache.quotes.reflect.*
    if debugMode then {
      report.warning(
        trace.mkString("\n")
          + "\n\n--------------------------------"
          + "\ngenerated code:\n\n"
          + cache.asTerm.show(using Printer.TreeCode)
      )
    }
  }
}
```

where `MyMacroVisitor` is an object implementing either full `TypeTreeVisitor` or a simplified `SimpleTypeTreeVisitor` to do the actual code-building business.

## Term-less iterator and visitor

When generated code does not need to access the actual value of the type, i.e. `valueTerm` then there is a faster and simpler `TypeTreeTermlessIterator` available accepting `TypeTreeTermlessVisitor`.

## How to work with typeclasses?

### Summoning existing typeclasses

The `TypeTreeVisitor` interface provides a method `maybeSummonTypeclassInstance`. Override this method with an implementation like below. If the instance of the typeclass is known for any of the types in your type tree it will be used instead of traversing the tree further down at this point.

```scala
inline override def maybeSummonTypeclassInstance(using
      cache: StatementsCache
  )(
    tpe: cache.quotes.reflect.TypeRepr, 
    valueTerm: cache.quotes.reflect.Term, 
    context: Context
  ): Option[Unit] = {
    given cache.quotes.type = cache.quotes
    import cache.quotes.reflect.*

    tpe.asType match {
      case '[a] =>
        Expr.summon[MyTypeclass[a]].map { instance =>
          cache.put(
            instance.asTerm.methodCall(
              "myMethod", // your typeclass method name
              List(
                ... // here goes your typeclass method parameters
              )
            )
          )
        }
    }
  }
```

### Deriving typeclass instances autonomously using macro

Your typeclass companion object can have the method deriving instances autonomously using the macro:

```scala
case class X() derives MyTypeclass

trait MyTypeclass[T]{
  def myMethod(...): XYZ
}

object MyTypeclass {

  import scala.quoted.*

  inline def derived[T]: MyTypeclass[T] = ${ derivedImpl[T] }

  private def derivedImpl[T: Type](using Quotes): Expr[MyTypeclass[T]] = {
    '{
      new MyTypeclass[T] {
        def myMethod(...): XYZ = // your typeclass method
          ${
            MyMacro.myMethodImpl[T](...)
          }
      }
    }
  }
```
where `MyMacro.myMethodImpl[T]` is a macro method using `TypeTreeIterator` with `MyMacroVisitor`.

### Providing typeclass instances semi-autonomously

Typeclass instances can derived semi-autonomously for each type
```scala
case class Y()
object Y {
  given MyTyepclass[Y] = new MyTypeclass[Y]{
    def myMethod(...): XYZ = // your typeclass method
      MyMacro.myMethod(...)
  }
}
```

### Preventing circular derivation calls

The `TypeTreeIterator.visitNode` method has a `summonTypeclassInstance` parameter which must set `false` for a toplevel call to prevent circular calls between macro method and typeclass derivation method. All the subsequent calls to the `visitNode` inside `TypeTreeIterator` will receive `summonTypeclassInstance = true`.

## Examples of macro

Working examples of macros built with `TypeTreeVisitor`:

### [XmlWriter](https://github.com/encalmo/xmlwriter)
Fast and resource-light XML serialization of any Scala types supporting `XmlWriter` typeclass.

### [StructuralRuntimeHashcode](StructuralRuntimeHashcode.scala)
Simple macro computing the hascode derived from the internal structure and actual names of the classes of the values, but not the values themselves. It allows to compare if two instances have the same runtime "shape".

```scala
import org.encalmo.utils.StructuralRuntimeHashcode

val instance = ...
val hashcode: Int = StructuralRuntimeHashcode.compute(instance)
```

### [ValuePathsList](ValuePathsList.scala)
Simple demo macro computing a list of all value paths avalilable in the current instance of some type.

```scala
import org.encalmo.utils.ValuePathsList

val instance = ...
val paths: List[String] = ValuePathsList.compute(instance)
println(paths.mkString("\n"))
```

### [InternalStructureHashcode](InternalStructureHashcode.scala)
Simple demo macro computing a hashcode that purely reflects a value's internal type structure, disregarding the actual field values. This is useful for comparing the shape or structure of different types at runtime.

```scala
import org.encalmo.utils.InternalStructureHashcode

val hashcode: Int = InternalStructureHashcode.compute[MyType]
println(hashcode)
```

### [DrawTypeTree](DrawTypeTree.scala)

Utility macro for visualizing the structure of any Scala type as an ASCII art tree, making it easy to understand nested structures and compositions in types at a glance. This is especially useful for debugging complex case classes, sealed trait hierarchies, tuples, collections, and other elaborate Scala types.

```scala
import org.encalmo.utils.DrawTypeTree

val structure: String = DrawTypeTree.draw[Person]
println(structure)
```

## Project content

```
├── .github
│   └── workflows
│       ├── pages.yaml
│       ├── release.yaml
│       └── test.yaml
│
├── .gitignore
├── .scalafmt.conf
├── DrawTypeTree.scala
├── DrawTypeTree.test.scala
├── Hashcode.scala
├── InternalStructureHashcode.scala
├── InternalStructureHashcode.test.scala
├── LICENSE
├── Order.java
├── project.scala
├── README.md
├── SimpleTypeTreeTermlessVisitor.scala
├── SimpleTypeTreeVisitor.scala
├── Status.java
├── StructuralRuntimeHashcode.scala
├── StructuralRuntimeHashcode.test.scala
├── TagName.scala
├── test.sh
├── TestModel.test.scala
├── TypeTreeIterator.scala
├── TypeTreeTermlessIterator.scala
├── TypeTreeTermlessVisitor.scala
├── TypeTreeVisitor.scala
├── ValuePathList.test.scala
└── ValuesPathList.scala
```

