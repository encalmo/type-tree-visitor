package org.encalmo.utils

import scala.quoted.Quotes
import scala.quoted.Expr
import scala.quoted.Type
import org.encalmo.utils.AnnotationUtils.AnnotationInfo

/** Compute the structural runtime hashcode of a value. It will take into account structure and actual types of the
  * nested values in runtime but not the values themselves.
  */
object StructuralRuntimeHashcode {

  private inline def debugMode = false

  inline def compute[T](value: T): Int = {
    ${ computeImpl[T]('{ value }) }
  }

  private def computeImpl[T: Type](expr: Expr[T])(using Quotes): Expr[Int] = {
    import cache.quotes.reflect.*
    given cache: StatementsCache = new StatementsCache
    computeUsingTypeTreeIterator(using cache)(TypeRepr.of[T], expr.asTerm)
    cache.asExprOf[Int]
  }

  private def computeUsingTypeTreeIterator(using
      cache: StatementsCache
  )(
      tpe: cache.quotes.reflect.TypeRepr,
      valueTerm: cache.quotes.reflect.Term
  ): Unit = {
    val context = new StructuralRuntimeHashcodeMacroContext
    val trace = scala.collection.mutable.Buffer.empty[String]

    context.initialize(using cache)
    TypeTreeIterator.visitNode(using cache, StructuralRuntimeHashcodeMacroVisitor)(
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

class StructuralRuntimeHashcodeMacroContext {

  def initialize(using cache: StatementsCache): Unit = {
    given cache.quotes.type = cache.quotes
    cache.getValueRefOfExpr[Hashcode]("hashcode", '{ new Hashcode() })
  }

  /** Update hashcode by each value's type name encountered in the type tree. */
  def updateHashcode(using cache: StatementsCache)(valueTerm: cache.quotes.reflect.Term): Unit = {
    cache.put(
      cache
        .getValueRef("hashcode")
        .methodCall(
          "update",
          List(
            valueTerm.methodCall("getClass", List()).methodCall("getName", List())
          )
        )
    )
  }

  def finalize(using cache: StatementsCache): Unit = {
    cache.put(cache.getValueRef("hashcode").methodCall("result", List()))
  }

  override def toString(): String = ""
}

object StructuralRuntimeHashcodeMacroVisitor extends SimpleTypeTreeVisitor {

  type Context = StructuralRuntimeHashcodeMacroContext

  /** Before visiting a node in the type tree. */
  inline override def beforeNode(using
      cache: StatementsCache
  )(
      tpe: cache.quotes.reflect.TypeRepr,
      valueTerm: cache.quotes.reflect.Term,
      annotations: Set[AnnotationInfo],
      isCollectionItem: Boolean,
      context: Context
  ): (Context, Set[AnnotationInfo]) =
    context.updateHashcode(valueTerm)
    (context, annotations)

}

/** A hashcode is a 32-bit integer used to uniquely identify an object during program execution. */
class Hashcode {

  val prime = 31
  var hashcode: Int = 17

  def update(any: Any): Unit = {
    hashcode = hashcode * prime + any.hashCode
  }

  def result: Int = hashcode

}
