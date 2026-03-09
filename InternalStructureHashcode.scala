package org.encalmo.utils

import scala.quoted.Quotes
import scala.quoted.Expr
import scala.quoted.Type
import org.encalmo.utils.AnnotationUtils.AnnotationInfo

/** Compute the internal structure hashcode of a type for a deep comparison if two types have the same fields and types.
  */
object InternalStructureHashcode {

  private inline def debugMode = false

  inline def compute[T]: Int = {
    ${ computeImpl[T] }
  }

  private def computeImpl[T: Type](using Quotes): Expr[Int] = {
    import cache.quotes.reflect.*
    given cache: StatementsCache = new StatementsCache
    computeUsingTypeTreeIterator(using cache)(TypeRepr.of[T])
    cache.asExprOf[Int]
  }

  private def computeUsingTypeTreeIterator(using
      cache: StatementsCache
  )(
      tpe: cache.quotes.reflect.TypeRepr
  ): Unit = {
    val context = new InternalStructureHashcodeMacroContext(firstNode = true)
    val trace = scala.collection.mutable.Buffer.empty[String]

    context.initialize(using cache)
    TypeTreeTermlessIterator.visitNode(using cache, InternalStructureHashcodeMacroVisitor)(
      tpe = tpe,
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

case class InternalStructureHashcodeMacroContext(firstNode: Boolean) {

  def initialize(using cache: StatementsCache): Unit = {
    given cache.quotes.type = cache.quotes
    cache.getValueRefOfExpr[Hashcode]("hashcode", '{ new Hashcode() })
  }

  /** Update hashcode by each value's type name encountered in the type tree. */
  def updateHashcode(using cache: StatementsCache)(s: String): Unit = {
    import cache.quotes.reflect.*
    cache.put(
      cache
        .getValueRef("hashcode")
        .methodCall("update", List(Literal(StringConstant(s))))
    )
  }

  def finalize(using cache: StatementsCache): Unit = {
    cache.put(cache.getValueRef("hashcode").methodCall("result", List()))
  }

  override def toString(): String = ""
}

object InternalStructureHashcodeMacroVisitor extends SimpleTypeTreeTermlessVisitor {

  type Context = InternalStructureHashcodeMacroContext

  /** Before visiting a node in the type tree. */
  inline override def beforeNode(using
      cache: StatementsCache
  )(
      tpe: cache.quotes.reflect.TypeRepr,
      annotations: Set[AnnotationInfo],
      isCollectionItem: Boolean,
      context: Context
  ): (Context, Set[AnnotationInfo]) =
    if context.firstNode
    then (context.copy(firstNode = false), annotations)
    else
      context.updateHashcode(tpe.show(using cache.quotes.reflect.Printer.TypeReprCode))
      (context, annotations)

  inline override def visitProductField(using
      cache: StatementsCache
  )(
      tpe: cache.quotes.reflect.TypeRepr,
      name: TagName,
      annotations: Set[AnnotationInfo],
      context: Context,
      visitNode: TypeTreeTermlessIterator.VisitNodeFunction
  ): Unit = {
    context.updateHashcode(name.show(using cache))
    visitNode(using cache, this)(
      tpe = tpe,
      annotations = annotations,
      isCollectionItem = false,
      context = context
    )
  }

}
