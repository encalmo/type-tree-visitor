package org.encalmo.utils

import scala.quoted.Quotes
import scala.quoted.Expr
import scala.quoted.Type
import org.encalmo.utils.AnnotationUtils.AnnotationInfo
import scala.collection.mutable.Buffer

object ValuesPathList {

  private inline def debugMode = false

  inline def compute[T](value: T): List[String] = {
    ${ computeImpl[T]('{ value }) }
  }

  private def computeImpl[T: Type](expr: Expr[T])(using Quotes): Expr[List[String]] = {
    import cache.quotes.reflect.*
    given cache: StatementsCache = new StatementsCache
    computeUsingTypeTreeIterator(using cache)(TypeRepr.of[T], expr.asTerm)
    cache.asExprOf[List[String]]
  }

  private def computeUsingTypeTreeIterator(using
      cache: StatementsCache
  )(
      tpe: cache.quotes.reflect.TypeRepr,
      valueTerm: cache.quotes.reflect.Term
  ): Unit = {
    val context = List.empty[String]
    val trace = scala.collection.mutable.Buffer.empty[String]

    import ValuesPathListMacroVisitor.*

    context.initialize(using cache)
    TypeTreeIterator.visitNode(using cache, ValuesPathListMacroVisitor)(
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

object ValuesPathListMacroVisitor extends SimpleTypeTreeVisitor {

  type Context = List[Any]

  override def beforeNode(using
      cache: StatementsCache
  )(
      tpe: cache.quotes.reflect.TypeRepr,
      valueTerm: cache.quotes.reflect.Term,
      annotations: Set[AnnotationInfo],
      isCollectionItem: Boolean,
      context: Context
  ): (Context, Set[AnnotationInfo]) =
    context.updateBuffer
    (context, annotations)

  override def visitProductField(using
      cache: StatementsCache
  )(
      tpe: cache.quotes.reflect.TypeRepr,
      name: TagName,
      valueTerm: cache.quotes.reflect.Term,
      annotations: Set[AnnotationInfo],
      context: Context,
      visitNode: TypeTreeIterator.VisitNodeFunction
  ): Unit = {
    visitNode(using cache, this)(
      tpe = tpe,
      valueTerm = valueTerm,
      annotations = annotations,
      isCollectionItem = false,
      context = name.resolve :: context
    )
  }

  override def visitCollectionItem(using
      cache: StatementsCache
  )(
      tpe: cache.quotes.reflect.TypeRepr,
      valueTerm: cache.quotes.reflect.Term,
      indexTerm: cache.quotes.reflect.Term,
      annotations: Set[AnnotationInfo],
      context: Context,
      visitNode: TypeTreeIterator.VisitNodeFunction
  ): Unit = {
    given cache.quotes.type = cache.quotes
    visitNode(using cache, this)(
      tpe = tpe,
      valueTerm = valueTerm,
      annotations = annotations,
      isCollectionItem = true,
      context = indexTerm :: context
    )
  }

  extension (context: Context) {

    def initialize(using cache: StatementsCache): Unit = {
      given cache.quotes.type = cache.quotes
      cache.getValueRefOfExpr[Buffer[String]]("buffer", '{ Buffer.empty[String] })
    }

    def updateBuffer(using cache: StatementsCache): Unit = {
      given cache.quotes.type = cache.quotes
      import cache.quotes.reflect.*
      val pathTerm = context.reverse.map(i => i.asInstanceOf[cache.quotes.reflect.Term]) match {
        case Nil         => StatementsCache.stringLiteral("")
        case head :: Nil => head
        case head :: tail =>
          tail.foldLeft(head) { (acc, term) =>
            acc.methodCall(
              "+",
              List(term.tpe.dealias.widen match {
                case tpe if tpe =:= TypeRepr.of[Int] =>
                  StringUtils.concat(
                    StatementsCache.stringLiteral("["),
                    term.applyToString,
                    StatementsCache.stringLiteral("]")
                  )
                case other =>
                  StringUtils.concat(
                    StatementsCache.stringLiteral("."),
                    term
                  )
              })
            )
          }
      }
      cache.put(
        cache.getValueRef("buffer").methodCall("append", List(pathTerm))
      )
    }

    def finalize(using cache: StatementsCache): Unit = {
      cache.put(cache.getValueRef("buffer").methodCall("toList", List()))
    }
  }

}
