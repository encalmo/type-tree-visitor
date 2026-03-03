<a href="https://central.sonatype.com/artifact/org.encalmo/type-tree-visitor_3" target="_blank">![Maven Central Version](https://img.shields.io/maven-central/v/org.encalmo/type-tree-visitor_3?style=for-the-badge)</a> <a href="https://encalmo.github.io/type-tree-visitor/scaladoc/org/encalmo/utils.html" target="_blank"><img alt="Scaladoc" src="https://img.shields.io/badge/docs-scaladoc-red?style=for-the-badge"></a>

# type-tree-visitor

## Motivation

This library provides `TypeTreeIterator` object and `TypeTreeVisitor` trait, two essential building blocks for Scala 3 macros aiming at efficiently and effortlessly deriving code based on some type tree traversal. The pattern implemented here has been developed for and later extracted from the [xmlwriter](https://github.com/encalmo/xmlwriter) macro codebase. While there are multiple established ways of implementing macros using inline/mirrors, or quotes/splices, or [hearth](https://github.com/MateuszKubuszok/hearth) library, there can be multiple reasons to use this toolset instead:

- familiar [visitor pattern](https://refactoring.guru/design-patterns/visitor)
- full support for manual, autonomous and semi-autonomous typeclass derivation modes
- algorithm split into fully implemented `iterator` object and open to extension `visitor` trait
- handy `StatementsCache` abstraction from [macro-utils](https://github.com/encalmo/macro-utils) instead of a bit problematic `Quotes`, with built-in support for nested scopes, reference and symbol cache, plus wrapping of a recurrent code in chunked methods on demand,
- good coverage of popular and new types:
   - Scala case classes, sealed traits, collections, arrays, enums, tuples, named tuples, selectables, named types, opaque types, and primitives
   - Java enums, records, maps, iterables, and primitives
- support for a custom type context instance carried over type tree iteration
- all (rechargable) batteries included

## Dependencies

- Scala >= 3.7.4

## Usage

Use with SBT

    libraryDependencies += "org.encalmo" %% "type-tree-visitor" % "0.9.0"

or with SCALA-CLI

    //> using dep org.encalmo::type-tree-visitor:0.9.0

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
      debugIndent = if debugMode then 0 else Int.MinValue
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

## Examples

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
