package org.encalmo.utils

import org.encalmo.utils.AnnotationUtils.*
import org.encalmo.utils.StatementsCache
import org.encalmo.utils.StatementsCache.*

import scala.quoted.*

/** Iterates over the structure (tree) of some Scala type without using the value term, just the type. Uses
  * TypeTreeTermlessVisitor instance to call custom logic for each node in the tree.
  */
object TypeTreeTermlessIterator {

  /** Function to visit a structure (tree) of some Scala type and value. */
  type VisitNodeFunction = (cache: StatementsCache, visitor: TypeTreeTermlessVisitor) ?=> (
      tpe: cache.quotes.reflect.TypeRepr,
      annotations: Set[AnnotationInfo],
      isCollectionItem: Boolean,
      context: visitor.Context
  ) => Unit

  /** Create delayed function to visit a node of some type and value. */
  private def visitNodeFunction(
      trace: scala.collection.mutable.Buffer[String],
      debugIndent: Int,
      summonTypeclassInstance: Boolean = true
  ): VisitNodeFunction =
    (cache: StatementsCache, visitor: TypeTreeTermlessVisitor) ?=>
      (
          tpe: cache.quotes.reflect.TypeRepr,
          annotations: Set[AnnotationInfo],
          isCollectionItem: Boolean,
          context: visitor.Context
      ) =>
        visitNode(using cache, visitor)(
          tpe = tpe,
          context = context,
          isCollectionItem = isCollectionItem,
          annotations = annotations,
          trace = trace,
          debugIndent = debugIndent,
          summonTypeclassInstance = summonTypeclassInstance
        )

  /** Recursive algorithm to visit value of some type and value */
  def visitNode(using
      cache: StatementsCache,
      visitor: TypeTreeTermlessVisitor
  )(
      tpe: cache.quotes.reflect.TypeRepr,
      context: visitor.Context,
      isCollectionItem: Boolean,
      annotations: Set[AnnotationInfo],
      trace: scala.collection.mutable.Buffer[String],
      debugIndent: Int,
      summonTypeclassInstance: Boolean = true
  ): Unit = {
    given cache.quotes.type = cache.quotes
    import cache.quotes.reflect.*

    val typeAnnotations = AnnotationUtils.annotationsOf(tpe)
    val allAnnotations = typeAnnotations ++ annotations

    val (context2, currentAnnotations) =
      visitor.beforeNode(
        tpe = tpe,
        annotations = allAnnotations,
        isCollectionItem = isCollectionItem,
        context = context
      )

    if (debugIndent >= 0) then
      trace.append(
        "  " * debugIndent
          + ":: "
          + context2
          + ": "
          + tpe.show(using Printer.TypeReprAnsiCode)
          + " " + allAnnotations.filter(_.name.contains("xml")).map(_.toString).mkString(", ")
      )

    def generateWriterExpressions: Unit = {
      tpe match {

        case TypeUtils.TypeReprIsPrimitiveOrStringOrBigDecimal() =>
          visitPrimitive(
            tpe = tpe,
            context = context2,
            isCollectionItem = isCollectionItem,
            trace = trace,
            debugIndent = debugIndent
          )

        case tpe if tpe.dealias =:= TypeRepr.of[BigInt] || tpe.dealias =:= TypeRepr.of[java.math.BigInteger] =>
          visitAsString(
            tpe = tpe,
            context = context2,
            isCollectionItem = isCollectionItem,
            trace = trace,
            debugIndent = debugIndent
          )

        case NamedTupleUtils.TypeReprIsNamedTuple() =>
          visitNamedTuple(
            tpe = tpe,
            context = context2,
            isCollectionItem = isCollectionItem,
            trace = trace,
            annotations = currentAnnotations,
            debugIndent = debugIndent
          )

        case TupleUtils.TypeReprIsTuple() =>
          visitTuple(
            tpe = tpe,
            context = context2,
            isCollectionItem = isCollectionItem,
            trace = trace,
            annotations = currentAnnotations,
            debugIndent = debugIndent
          )

        // Handle Option[T] special way, omit the element if the value is None
        case OptionUtils.TypeReprIsOption(tpe) =>
          visitOption(
            tpe = tpe,
            context = context2,
            isCollectionItem = isCollectionItem,
            trace = trace,
            annotations = currentAnnotations,
            debugIndent = debugIndent
          )

        case EitherUtils.TypeReprIsEither(leftTpe, rightTpe) =>
          visitEither(
            tpe = tpe,
            leftTpe = leftTpe,
            rightTpe = rightTpe,
            context = context2,
            isCollectionItem = isCollectionItem,
            trace = trace,
            annotations = currentAnnotations,
            debugIndent = debugIndent
          )

        case MapUtils.TypeReprIsMap(keyTpe, valueTpe) =>
          visitMap(
            tpe = tpe,
            keyTpe = keyTpe,
            valueTpe = valueTpe,
            context = context2,
            isCollectionItem = isCollectionItem,
            trace = trace,
            annotations = currentAnnotations,
            debugIndent = debugIndent
          )

        case IterableUtils.TypeReprIsIterable(itemTpe) =>
          visitCollection(
            tpe = tpe,
            itemTpe = itemTpe,
            context = context2,
            isCollectionItem = isCollectionItem,
            trace = trace,
            annotations = currentAnnotations,
            debugIndent = debugIndent
          )

        case ArrayUtils.TypeReprIsArray(itemTpe) =>
          visitArray(
            tpe = tpe,
            itemTpe = itemTpe,
            context = context2,
            isCollectionItem = isCollectionItem,
            trace = trace,
            annotations = currentAnnotations,
            debugIndent = debugIndent
          )

        case CaseClassUtils.TypeReprIsCaseClass() => {
          visitCaseClass(
            tpe = tpe,
            context = context2,
            isCollectionItem = isCollectionItem,
            trace = trace,
            annotations = currentAnnotations,
            debugIndent = debugIndent
          )
        }

        case EnumUtils.TypeReprIsEnum() =>
          visitEnum(
            tpe = tpe,
            context = context2,
            isCollectionItem = isCollectionItem,
            trace = trace,
            annotations = currentAnnotations,
            debugIndent = debugIndent
          )

        case UnionUtils.TypeReprIsUnion(_) =>
          visitUnion(
            tpe = tpe,
            context = context2,
            isCollectionItem = isCollectionItem,
            trace = trace,
            annotations = currentAnnotations,
            debugIndent = debugIndent
          )

        case SelectableUtils.TypeReprIsSelectable(fields) =>
          visitSelectable(
            tpe = tpe,
            fields = fields,
            context = context2,
            isCollectionItem = isCollectionItem,
            trace = trace,
            annotations = currentAnnotations,
            debugIndent = debugIndent
          )

        case OpaqueTypeUtils.TypeReprIsOpaqueType(upperBoundTpe) =>
          visitOpaqueType(
            tpe = tpe,
            upperBoundTpe = upperBoundTpe,
            context = context2,
            isCollectionItem = isCollectionItem,
            trace = trace,
            annotations = currentAnnotations,
            debugIndent = debugIndent
          )

        case JavaRecordUtils.TypeReprIsJavaRecord() =>
          visitJavaRecord(
            tpe = tpe,
            context = context2,
            isCollectionItem = isCollectionItem,
            trace = trace,
            annotations = currentAnnotations,
            debugIndent = debugIndent
          )

        case JavaMapUtils.TypeReprIsJavaMap(keyTpe, valueTpe) =>
          visitJavaMap(
            tpe = tpe,
            keyTpe = keyTpe,
            valueTpe = valueTpe,
            context = context2,
            isCollectionItem = isCollectionItem,
            trace = trace,
            annotations = currentAnnotations,
            debugIndent = debugIndent
          )

        case JavaIterableUtils.TypeReprIsJavaIterable(itemTpe) =>
          visitJavaIterable(
            tpe = tpe,
            itemTpe = itemTpe,
            context = context2,
            isCollectionItem = isCollectionItem,
            trace = trace,
            annotations = currentAnnotations,
            debugIndent = debugIndent
          )

        case _ =>
          // default to the string representation of the value
          visitAsString(
            tpe = tpe,
            context = context2,
            isCollectionItem = isCollectionItem,
            trace = trace,
            debugIndent = debugIndent
          )
      }
    }

    def maybeProcessNodeDirectly: Unit = {
      visitor
        .maybeProcessNodeDirectly(
          tpe = tpe,
          annotations = currentAnnotations,
          isCollectionItem = isCollectionItem,
          context = context2,
          visitNode = visitNodeFunction(trace, debugIndent + 1)
        )
        .getOrElse {
          generateWriterExpressions
        }
    }

    if (summonTypeclassInstance)
    then
      visitor
        .maybeSummonTypeclassInstance(tpe, context2)
        .getOrElse(maybeProcessNodeDirectly)
    else maybeProcessNodeDirectly

    visitor.afterNode(allAnnotations, context)
  }

  def visitPrimitive(using
      cache: StatementsCache,
      visitor: TypeTreeTermlessVisitor
  )(
      tpe: cache.quotes.reflect.TypeRepr,
      context: visitor.Context,
      isCollectionItem: Boolean,
      trace: scala.collection.mutable.Buffer[String],
      debugIndent: Int
  ): Unit = {
    debug(trace, debugIndent, tpe, "visitPrimitive")
    visitor.visitPrimitive(tpe, context)
  }

  def visitAsString(using
      cache: StatementsCache,
      visitor: TypeTreeTermlessVisitor
  )(
      tpe: cache.quotes.reflect.TypeRepr,
      context: visitor.Context,
      isCollectionItem: Boolean,
      trace: scala.collection.mutable.Buffer[String],
      debugIndent: Int
  ): Unit = {
    debug(trace, debugIndent, tpe, "visitAsString")
    visitor.visitAsString(tpe, context)
  }

  def visitCaseClass(using
      cache: StatementsCache,
      visitor: TypeTreeTermlessVisitor
  )(
      tpe: cache.quotes.reflect.TypeRepr,
      isCollectionItem: Boolean,
      trace: scala.collection.mutable.Buffer[String],
      annotations: Set[AnnotationInfo],
      debugIndent: Int,
      context: visitor.Context
  ): Unit = {
    given cache.quotes.type = cache.quotes
    import cache.quotes.reflect.*
    debug(trace, debugIndent, tpe, "visitCaseClass")

    val context2 = visitor.beforeCaseClass(tpe, annotations, context)

    cache.putParamlessMethodCallOf[Unit](
      methodName = createMethodName("CaseClass", tpe, annotations),
      minMethodLinesCount = 6,
      buildMethodBody = (nested: StatementsCache) ?=>
        given nested.quotes.type = nested.quotes
        CaseClassUtils.visitTermless(
          tpe = tpe.toTypeRepr,
          functionOnField = { (tpe, name, annotations) =>
            visitor
              .visitCaseClassField(
                tpe = tpe,
                name = name,
                annotations = annotations,
                context = context2,
                visitNode = visitNodeFunction(trace, debugIndent + 1)
              )
          }
        )
      ,
      scope = StatementsCache.Scope.TopLevel
    )

    visitor.afterCaseClass(tpe, context)
  }

  def visitEnum(using
      outer: StatementsCache,
      visitor: TypeTreeTermlessVisitor
  )(
      tpe: outer.quotes.reflect.TypeRepr,
      context: visitor.Context,
      isCollectionItem: Boolean,
      trace: scala.collection.mutable.Buffer[String],
      annotations: Set[AnnotationInfo],
      debugIndent: Int
  ): Unit = {
    debug(trace, debugIndent, tpe, "visitEnum")
    given outer.quotes.type = outer.quotes
    import outer.quotes.reflect.*
    val context2 = visitor.beforeEnum(tpe, annotations, context)
    outer.putParamlessMethodCallOf[Unit](
      createMethodName("Enum", tpe, annotations),
      (cache: StatementsCache) ?=>
        given cache.quotes.type = cache.quotes
        EnumUtils.visitTermless(
          tpe = tpe.toTypeRepr,
          functionWhenCaseValue = { (tpe, name, caseAnnotations) =>
            visitor.visitEnumCaseValue(
              tpe = tpe.toTypeRepr,
              name = name,
              annotations = annotations ++ caseAnnotations,
              isCollectionItem = isCollectionItem,
              context = context2,
              visitNode = visitNodeFunction(trace, debugIndent + 1)
            )
          },
          functionWhenCaseClass = { (tpe, name, caseAnnotations) =>
            visitor.visitEnumCaseClass(
              tpe = tpe.toTypeRepr,
              name = name,
              annotations = annotations ++ caseAnnotations,
              isCollectionItem = isCollectionItem,
              context = context2,
              visitNode = visitNodeFunction(trace, debugIndent + 1)
            )
          }
        )
      ,
      scope = StatementsCache.Scope.TopLevel
    )
    visitor.afterEnum(tpe, context)
  }

  def visitUnion(using
      outer: StatementsCache,
      visitor: TypeTreeTermlessVisitor
  )(
      tpe: outer.quotes.reflect.TypeRepr,
      context: visitor.Context,
      isCollectionItem: Boolean,
      trace: scala.collection.mutable.Buffer[String],
      annotations: Set[AnnotationInfo],
      debugIndent: Int
  ): Unit = {
    debug(trace, debugIndent, tpe, "visitUnion")
    given outer.quotes.type = outer.quotes
    val context2 = visitor.beforeUnion(tpe, annotations, context)
    UnionUtils.visitTermless(
      tpe = tpe.toTypeRepr,
      functionOnCase = { tpe =>
        visitor.visitUnionMember(
          tpe = tpe.toTypeRepr,
          annotations = annotations,
          isCollectionItem = isCollectionItem,
          context = context2,
          visitNode = visitNodeFunction(trace, debugIndent + 1)
        )
      }
    )
    visitor.afterUnion(tpe, context)
  }

  def visitOption(using
      cache: StatementsCache,
      visitor: TypeTreeTermlessVisitor
  )(
      tpe: cache.quotes.reflect.TypeRepr,
      context: visitor.Context,
      isCollectionItem: Boolean,
      trace: scala.collection.mutable.Buffer[String],
      annotations: Set[AnnotationInfo],
      debugIndent: Int
  ): Unit = {
    debug(trace, debugIndent, tpe, "visitOption")
    visitNode(using cache, visitor)(
      tpe = tpe,
      context = context,
      isCollectionItem = isCollectionItem,
      annotations = annotations,
      trace = trace,
      debugIndent = debugIndent
    )
  }

  def visitEither(using
      cache: StatementsCache,
      visitor: TypeTreeTermlessVisitor
  )(
      tpe: cache.quotes.reflect.TypeRepr,
      leftTpe: cache.quotes.reflect.TypeRepr,
      rightTpe: cache.quotes.reflect.TypeRepr,
      context: visitor.Context,
      isCollectionItem: Boolean,
      trace: scala.collection.mutable.Buffer[String],
      annotations: Set[AnnotationInfo],
      debugIndent: Int
  ): Unit = {
    debug(trace, debugIndent, tpe, "visitEither")
    visitNode(using cache, visitor)(
      tpe = leftTpe,
      context = context,
      isCollectionItem = isCollectionItem,
      annotations = annotations,
      trace = trace,
      debugIndent = debugIndent
    )
    visitNode(using cache, visitor)(
      tpe = rightTpe,
      context = context,
      isCollectionItem = isCollectionItem,
      annotations = annotations,
      trace = trace,
      debugIndent = debugIndent
    )
  }

  def visitCollection(using
      cache: StatementsCache,
      visitor: TypeTreeTermlessVisitor
  )(
      tpe: cache.quotes.reflect.TypeRepr,
      itemTpe: cache.quotes.reflect.TypeRepr,
      context: visitor.Context,
      isCollectionItem: Boolean,
      trace: scala.collection.mutable.Buffer[String],
      annotations: Set[AnnotationInfo],
      debugIndent: Int
  ): Unit = {
    debug(trace, debugIndent, tpe, "visitCollection")
    val context2 = visitor.beforeCollection(tpe, itemTpe, annotations, context)
    visitor.visitCollectionItem(
      tpe = itemTpe,
      annotations = annotations,
      context = context2,
      visitNode = visitNodeFunction(trace, debugIndent + 1)
    )
    visitor.afterCollection(tpe, context)
  }

  def visitArray(using
      cache: StatementsCache,
      visitor: TypeTreeTermlessVisitor
  )(
      tpe: cache.quotes.reflect.TypeRepr,
      itemTpe: cache.quotes.reflect.TypeRepr,
      context: visitor.Context,
      isCollectionItem: Boolean,
      trace: scala.collection.mutable.Buffer[String],
      annotations: Set[AnnotationInfo],
      debugIndent: Int
  ): Unit = {
    debug(trace, debugIndent, tpe, "visitArray")
    given cache.quotes.type = cache.quotes
    val context2 = visitor.beforeArray(tpe, itemTpe, annotations, context)
    visitor.visitArrayItem(
      tpe = itemTpe,
      annotations = annotations,
      context = context2,
      visitNode = visitNodeFunction(trace, debugIndent + 1)
    )
    visitor.afterArray(tpe, context)
  }

  def visitMap(using
      cache: StatementsCache,
      visitor: TypeTreeTermlessVisitor
  )(
      tpe: cache.quotes.reflect.TypeRepr,
      keyTpe: cache.quotes.reflect.TypeRepr,
      valueTpe: cache.quotes.reflect.TypeRepr,
      context: visitor.Context,
      isCollectionItem: Boolean,
      trace: scala.collection.mutable.Buffer[String],
      annotations: Set[AnnotationInfo],
      debugIndent: Int
  ): Unit = {
    debug(trace, debugIndent, tpe, "visitMap")
    given cache.quotes.type = cache.quotes
    val context2 = visitor.beforeMap(tpe, keyTpe, valueTpe, annotations, context)
    visitor.visitMapEntry(
      tpe = valueTpe,
      annotations = annotations,
      context = context2,
      visitNode = visitNodeFunction(trace, debugIndent + 1)
    )
    visitor.afterMap(tpe, context)
  }

  def visitJavaIterable(using
      cache: StatementsCache,
      visitor: TypeTreeTermlessVisitor
  )(
      tpe: cache.quotes.reflect.TypeRepr,
      itemTpe: cache.quotes.reflect.TypeRepr,
      context: visitor.Context,
      isCollectionItem: Boolean,
      trace: scala.collection.mutable.Buffer[String],
      annotations: Set[AnnotationInfo],
      debugIndent: Int
  ): Unit = {
    debug(trace, debugIndent, tpe, "visitJavaIterable")
    given cache.quotes.type = cache.quotes
    val context2 = visitor.beforeJavaIterable(tpe, itemTpe, annotations, context)
    visitor.visitJavaIterableItem(
      tpe = itemTpe,
      annotations = annotations,
      context = context2,
      visitNode = visitNodeFunction(trace, debugIndent + 1)
    )
    visitor.afterJavaIterable(tpe, context)
  }

  def visitJavaMap(using
      cache: StatementsCache,
      visitor: TypeTreeTermlessVisitor
  )(
      tpe: cache.quotes.reflect.TypeRepr,
      keyTpe: cache.quotes.reflect.TypeRepr,
      valueTpe: cache.quotes.reflect.TypeRepr,
      context: visitor.Context,
      isCollectionItem: Boolean,
      trace: scala.collection.mutable.Buffer[String],
      annotations: Set[AnnotationInfo],
      debugIndent: Int
  ): Unit = {
    debug(trace, debugIndent, tpe, "visitJavaMap")
    given cache.quotes.type = cache.quotes
    val context2 = visitor.beforeJavaMap(tpe, keyTpe, valueTpe, annotations, context)
    visitor.visitJavaMapEntry(
      tpe = valueTpe,
      annotations = annotations,
      context = context2,
      visitNode = visitNodeFunction(trace, debugIndent + 1)
    )
    visitor.afterJavaMap(tpe, context)
  }

  /** Write Scala tupes */
  def visitTuple(using
      cache: StatementsCache,
      visitor: TypeTreeTermlessVisitor
  )(
      tpe: cache.quotes.reflect.TypeRepr,
      context: visitor.Context,
      isCollectionItem: Boolean,
      trace: scala.collection.mutable.Buffer[String],
      annotations: Set[AnnotationInfo],
      debugIndent: Int
  ): Unit = {
    debug(trace, debugIndent, tpe, "visitTuple")
    val context2 = visitor.beforeTuple(tpe, annotations, context)
    TupleUtils.visitTermless(
      tpe.toTypeRepr,
      functionOnItem = { (tpe, index) =>
        visitor.visitTupleItem(
          tpe = tpe.toTypeRepr,
          annotations = annotations,
          context = context2,
          visitNode = visitNodeFunction(trace, debugIndent + 1)
        )
      }
    )
    visitor.afterTuple(tpe, context)
  }

  /** Write Scala named tuples */
  def visitNamedTuple(using
      cache: StatementsCache,
      visitor: TypeTreeTermlessVisitor
  )(
      tpe: cache.quotes.reflect.TypeRepr,
      context: visitor.Context,
      isCollectionItem: Boolean,
      trace: scala.collection.mutable.Buffer[String],
      annotations: Set[AnnotationInfo],
      debugIndent: Int
  ): Unit = {
    debug(trace, debugIndent, tpe, "visitNamedTuple")
    val context2 = visitor.beforeNamedTuple(tpe, annotations, context)
    NamedTupleUtils.visitTermless(
      tpe.toTypeRepr,
      functionOnField = { (tpe, name, index) =>
        visitor.visitNamedTupleItem(
          tpe = tpe.toTypeRepr,
          name = name,
          annotations = annotations,
          context = context2,
          visitNode = visitNodeFunction(trace, debugIndent + 1)
        )
      }
    )
    visitor.afterNamedTuple(tpe, context)
  }

  /** Write Scala structural types and objects extending `Selectable` with a `Fields` member type. */
  def visitSelectable(using
      cache: StatementsCache,
      visitor: TypeTreeTermlessVisitor
  )(
      tpe: cache.quotes.reflect.TypeRepr,
      fields: cache.quotes.reflect.TypeRepr,
      context: visitor.Context,
      isCollectionItem: Boolean,
      trace: scala.collection.mutable.Buffer[String],
      annotations: Set[AnnotationInfo],
      debugIndent: Int
  ): Unit = {
    debug(trace, debugIndent, tpe, "visitSelectable")
    given cache.quotes.type = cache.quotes
    val context2 = visitor.beforeSelectable(tpe, fields, annotations, context)
    cache.putParamlessMethodCallOf[Unit](
      createMethodName("Selectable", tpe, annotations),
      (nested: StatementsCache) ?=>
        SelectableUtils.visitFieldsTermless(
          fieldsTpe = fields.toTypeRepr,
          functionOnField = { (tpe, name) =>
            visitor.visitSelectableField(
              tpe = tpe.toTypeRepr,
              name = name,
              annotations = annotations,
              context = context2,
              visitNode = visitNodeFunction(trace, debugIndent + 1)
            )
          }
        ),
      scope = StatementsCache.Scope.TopLevel
    )
    visitor.afterSelectable(tpe, context)
  }

  def visitJavaRecord(using
      cache: StatementsCache,
      visitor: TypeTreeTermlessVisitor
  )(
      tpe: cache.quotes.reflect.TypeRepr,
      context: visitor.Context,
      isCollectionItem: Boolean,
      trace: scala.collection.mutable.Buffer[String],
      annotations: Set[AnnotationInfo],
      debugIndent: Int
  ): Unit = {
    given cache.quotes.type = cache.quotes
    import cache.quotes.reflect.*
    debug(trace, debugIndent, tpe, "visitJavaRecord")

    val context2 = visitor.beforeJavaRecord(tpe, annotations, context)
    cache.putParamlessMethodCallOf[Unit](
      createMethodName("Record", tpe, annotations),
      (nested: StatementsCache) ?=>
        JavaRecordUtils.visitTermless(
          tpe = tpe.toTypeRepr,
          functionOnField = { (tpe, name) =>
            visitor.visitJavaRecordField(
              tpe = tpe,
              name = name,
              annotations = annotations,
              context = context2,
              visitNode = visitNodeFunction(trace, debugIndent + 1)
            )
          }
        ),
      scope = StatementsCache.Scope.TopLevel
    )
    visitor.afterJavaRecord(tpe, context)
  }

  /** Write Scala opaque types, if there is an upper bound, write the upper bound, otherwise write the string
    * representation of the value.
    */
  def visitOpaqueType(using
      cache: StatementsCache,
      visitor: TypeTreeTermlessVisitor
  )(
      tpe: cache.quotes.reflect.TypeRepr,
      upperBoundTpe: Option[cache.quotes.reflect.TypeRepr],
      context: visitor.Context,
      isCollectionItem: Boolean,
      trace: scala.collection.mutable.Buffer[String],
      annotations: Set[AnnotationInfo],
      debugIndent: Int
  ): Unit = {
    debug(trace, debugIndent, tpe, "visitOpaqueType")
    visitor.visitOpaqueType(
      tpe = tpe,
      upperBoundTpe = upperBoundTpe,
      annotations = annotations,
      isCollectionItem = isCollectionItem,
      context = context,
      visitNode = visitNodeFunction(trace, debugIndent + 1)
    )
  }

  inline def debug(using
      cache: StatementsCache
  )(
      trace: scala.collection.mutable.Buffer[String],
      debugIndent: Int,
      tpe: cache.quotes.reflect.TypeRepr,
      message: String
  ): Unit = {
    if (debugIndent >= 0) then trace.append("  " * debugIndent + " > " + message)
  }

  inline def createMethodName(using
      cache: StatementsCache
  )(inline name: String, tpe: cache.quotes.reflect.TypeRepr, annotations: Set[AnnotationInfo]): String = {
    "visit" + name + "_" + TypeNameUtils
      .underscored(tpe.show(using cache.quotes.reflect.Printer.TypeReprCode))
      .replace("[", "__")
      .replace("]", "")
      .replace(",", "__")
      + annotations.hash(_.contains(".xml")).replace("-", "")
  }
}
