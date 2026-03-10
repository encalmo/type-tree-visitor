package org.encalmo.utils

import org.encalmo.utils.StatementsCache
import org.encalmo.utils.AnnotationUtils.AnnotationInfo

/** Instance of this trait visits the structure (tree) of some Scala type without using the value term.
  * @see
  *   https://refactoring.guru/design-patterns/visitor
  */
trait SimpleTypeTreeTermlessVisitor extends TypeTreeTermlessVisitor {

  /** Before visiting a product type node in the type tree. */
  def beforeProduct(using
      cache: StatementsCache
  )(
      tpe: cache.quotes.reflect.TypeRepr,
      annotations: Set[AnnotationInfo],
      context: Context
  ): Context = {
    context
  }

  /** Visit a field of a product type node in the type tree. Default implementation just visits the node without any
    * special processing.
    */
  def visitProductField(using
      cache: StatementsCache
  )(
      tpe: cache.quotes.reflect.TypeRepr,
      name: TagName,
      annotations: Set[AnnotationInfo],
      context: Context,
      visitNode: TypeTreeTermlessIterator.VisitNodeFunction
  ): Unit =
    visitNode(using cache, this)(
      tpe = tpe,
      annotations = annotations,
      isCollectionItem = false,
      context = context
    )

  /** After visiting a product type node in the type tree. */
  def afterProduct(using
      cache: StatementsCache
  )(
      tpe: cache.quotes.reflect.TypeRepr,
      context: Context
  ): Unit = {}

  /** Before visiting a sum type node in the type tree. */
  def beforeSum(using
      cache: StatementsCache
  )(
      tpe: cache.quotes.reflect.TypeRepr,
      annotations: Set[AnnotationInfo],
      context: Context
  ): Context = {
    context
  }

  /** Visit a case of a sum type node in the type tree. Default implementation just visits the node without any special
    * processing.
    */
  def visitSumCase(using
      cache: StatementsCache
  )(
      tpe: cache.quotes.reflect.TypeRepr,
      name: String,
      annotations: Set[AnnotationInfo],
      isCollectionItem: Boolean,
      context: Context,
      visitNode: TypeTreeTermlessIterator.VisitNodeFunction
  ): Unit =
    given cache.quotes.type = cache.quotes
    visitNode(using cache, this)(
      tpe = tpe,
      annotations = annotations,
      isCollectionItem = isCollectionItem,
      context = context
    )

  /** After visiting a sum type node in the type tree. */
  def afterSum(using
      cache: StatementsCache
  )(
      tpe: cache.quotes.reflect.TypeRepr,
      context: Context
  ): Unit = {}

  /** Create a prefix to use for some intermediate variables */
  def createVariableNamePrefix(using cache: StatementsCache)(context: Context): String =
    "it"

  /** Before visiting a node in the type tree. */
  def beforeNode(using
      cache: StatementsCache
  )(
      tpe: cache.quotes.reflect.TypeRepr,
      annotations: Set[AnnotationInfo],
      isCollectionItem: Boolean,
      context: Context
  ): (Context, Set[AnnotationInfo]) = {
    (context, annotations)
  }

  /** After visiting a node in the type tree. */
  def afterNode(using cache: StatementsCache)(annotations: Set[AnnotationInfo], context: Context): Unit = {}

  /** Maybe process the node directly without walking the tree further. */
  def maybeProcessNodeDirectly(using
      cache: StatementsCache
  )(
      tpe: cache.quotes.reflect.TypeRepr,
      annotations: Set[AnnotationInfo],
      isCollectionItem: Boolean,
      context: Context,
      visitNode: TypeTreeTermlessIterator.VisitNodeFunction
  ): Option[Unit] = None

  /** Maybe summon an existing typeclass instance to process the node instead of walking the tree further. */
  def maybeSummonTypeclassInstance(using
      cache: StatementsCache
  )(
      tpe: cache.quotes.reflect.TypeRepr,
      context: Context
  ): Option[Unit] = None

  /** Visit a node represented by a primitive-like values, including BigDecimal */
  def visitPrimitive(using
      cache: StatementsCache
  )(
      tpe: cache.quotes.reflect.TypeRepr,
      context: Context
  ): Unit = {}

  /** Visit a node represented by a string value. */
  def visitAsString(using
      cache: StatementsCache
  )(
      tpe: cache.quotes.reflect.TypeRepr,
      context: Context
  ): Unit = {}

  /** Visit a node holding an opaque type value. */
  def visitOpaqueType(using
      cache: StatementsCache
  )(
      tpe: cache.quotes.reflect.TypeRepr,
      upperBoundTpe: Option[cache.quotes.reflect.TypeRepr],
      annotations: Set[AnnotationInfo],
      isCollectionItem: Boolean,
      context: Context,
      visitNode: TypeTreeTermlessIterator.VisitNodeFunction
  ): Unit = {
    given cache.quotes.type = cache.quotes
    upperBoundTpe match {
      case Some(upperBoundTpe) =>
        visitNode(using cache, this)(
          tpe = upperBoundTpe,
          annotations = annotations,
          isCollectionItem = isCollectionItem,
          context = context
        )

      case None =>
        visitAsString(
          tpe = tpe,
          context = context
        )
    }
  }

  /** Before visiting a case class node in the type tree. */
  def beforeCaseClass(using
      cache: StatementsCache
  )(
      tpe: cache.quotes.reflect.TypeRepr,
      annotations: Set[AnnotationInfo],
      context: Context
  ): Context =
    beforeProduct(tpe, annotations, context)

  /** Visit a field of a case class node in the type tree. */
  def visitCaseClassField(using
      cache: StatementsCache
  )(
      tpe: cache.quotes.reflect.TypeRepr,
      name: String,
      annotations: Set[AnnotationInfo],
      context: Context,
      visitNode: TypeTreeTermlessIterator.VisitNodeFunction
  ): Unit =
    visitProductField(tpe, name, annotations, context, visitNode)

  /** After visiting a case class node in the type tree. */
  def afterCaseClass(using
      cache: StatementsCache
  )(
      tpe: cache.quotes.reflect.TypeRepr,
      context: Context
  ): Unit =
    afterProduct(tpe, context)

  /** Before visiting an enum node in the type tree. */
  def beforeEnum(using
      cache: StatementsCache
  )(
      tpe: cache.quotes.reflect.TypeRepr,
      annotations: Set[AnnotationInfo],
      context: Context
  ): Context =
    beforeSum(tpe, annotations, context)

  /** Visit a case value of an enum node in the type tree. */
  def visitEnumCaseValue(using
      cache: StatementsCache
  )(
      tpe: cache.quotes.reflect.TypeRepr,
      name: String,
      annotations: Set[AnnotationInfo],
      isCollectionItem: Boolean,
      context: Context,
      visitNode: TypeTreeTermlessIterator.VisitNodeFunction
  ): Unit =
    visitAsString(tpe, context)

  /** Visit a case class of an enum node in the type tree. */
  def visitEnumCaseClass(using
      cache: StatementsCache
  )(
      tpe: cache.quotes.reflect.TypeRepr,
      name: String,
      annotations: Set[AnnotationInfo],
      isCollectionItem: Boolean,
      context: Context,
      visitNode: TypeTreeTermlessIterator.VisitNodeFunction
  ): Unit =
    visitSumCase(tpe, name, annotations, isCollectionItem, context, visitNode)

  /** After visiting an enum node in the type tree. */
  def afterEnum(using
      cache: StatementsCache
  )(
      tpe: cache.quotes.reflect.TypeRepr,
      context: Context
  ): Unit =
    afterSum(tpe, context)

  /** Before visiting a collection node in the type tree. */
  def beforeCollection(using
      cache: StatementsCache
  )(
      tpe: cache.quotes.reflect.TypeRepr,
      itemTpe: cache.quotes.reflect.TypeRepr,
      annotations: Set[AnnotationInfo],
      context: Context
  ): Context = context

  /** Visit an item of a collection node in the type tree. */
  def visitCollectionItem(using
      cache: StatementsCache
  )(
      tpe: cache.quotes.reflect.TypeRepr,
      annotations: Set[AnnotationInfo],
      context: Context,
      visitNode: TypeTreeTermlessIterator.VisitNodeFunction
  ): Unit =
    visitNode(using cache, this)(
      tpe = tpe,
      annotations = annotations,
      isCollectionItem = true,
      context = context
    )

  /** After visiting a collection node in the type tree. */
  def afterCollection(using
      cache: StatementsCache
  )(
      tpe: cache.quotes.reflect.TypeRepr,
      context: Context
  ): Unit = {}

  /** Before visiting an array node in the type tree. */
  def beforeArray(using
      cache: StatementsCache
  )(
      tpe: cache.quotes.reflect.TypeRepr,
      itemTpe: cache.quotes.reflect.TypeRepr,
      annotations: Set[AnnotationInfo],
      context: Context
  ): Context =
    beforeCollection(tpe, itemTpe, annotations, context)

  /** Visit an item of an array node in the type tree. */
  def visitArrayItem(using
      cache: StatementsCache
  )(
      tpe: cache.quotes.reflect.TypeRepr,
      annotations: Set[AnnotationInfo],
      context: Context,
      visitNode: TypeTreeTermlessIterator.VisitNodeFunction
  ): Unit =
    visitCollectionItem(tpe, annotations, context, visitNode)

  /** After visiting an array node in the type tree. */
  def afterArray(using
      cache: StatementsCache
  )(
      tpe: cache.quotes.reflect.TypeRepr,
      context: Context
  ): Unit =
    afterCollection(tpe, context)

  /** Before visiting a tuple node in the type tree. */
  def beforeTuple(using
      cache: StatementsCache
  )(
      tpe: cache.quotes.reflect.TypeRepr,
      annotations: Set[AnnotationInfo],
      context: Context
  ): Context =
    beforeCollection(tpe, tpe, annotations, context)

  /** Visit an item of a tuple node in the type tree. */
  def visitTupleItem(using
      cache: StatementsCache
  )(
      tpe: cache.quotes.reflect.TypeRepr,
      annotations: Set[AnnotationInfo],
      context: Context,
      visitNode: TypeTreeTermlessIterator.VisitNodeFunction
  ): Unit =
    visitCollectionItem(tpe, annotations, context, visitNode)

  /** After visiting a tuple node in the type tree. */
  def afterTuple(using
      cache: StatementsCache
  )(
      tpe: cache.quotes.reflect.TypeRepr,
      context: Context
  ): Unit =
    afterCollection(tpe, context)

  /** Before visiting a named tuple node in the type tree. */
  def beforeNamedTuple(using
      cache: StatementsCache
  )(
      tpe: cache.quotes.reflect.TypeRepr,
      annotations: Set[AnnotationInfo],
      context: Context
  ): Context =
    beforeProduct(tpe, annotations, context)

  /** Visit a field of a named tuple node in the type tree. */
  def visitNamedTupleItem(using
      cache: StatementsCache
  )(
      tpe: cache.quotes.reflect.TypeRepr,
      name: String,
      annotations: Set[AnnotationInfo],
      context: Context,
      visitNode: TypeTreeTermlessIterator.VisitNodeFunction
  ): Unit =
    visitProductField(tpe, name, annotations, context, visitNode)

  /** After visiting a namedtuple node in the type tree. */
  def afterNamedTuple(using
      cache: StatementsCache
  )(
      tpe: cache.quotes.reflect.TypeRepr,
      context: Context
  ): Unit =
    afterProduct(tpe, context)

  /** Before visiting a Selectable node in the type tree. */
  def beforeSelectable(using
      cache: StatementsCache
  )(
      tpe: cache.quotes.reflect.TypeRepr,
      fields: cache.quotes.reflect.TypeRepr,
      annotations: Set[AnnotationInfo],
      context: Context
  ): Context =
    beforeProduct(tpe, annotations, context)

  /** Visit a field of a Selectable node in the type tree. */
  def visitSelectableField(using
      cache: StatementsCache
  )(
      tpe: cache.quotes.reflect.TypeRepr,
      name: String,
      annotations: Set[AnnotationInfo],
      context: Context,
      visitNode: TypeTreeTermlessIterator.VisitNodeFunction
  ): Unit =
    visitProductField(tpe, name, annotations, context, visitNode)

  /** After visiting a Selectable node in the type tree. */
  def afterSelectable(using
      cache: StatementsCache
  )(
      tpe: cache.quotes.reflect.TypeRepr,
      context: Context
  ): Unit =
    afterProduct(tpe, context)

  /** Visit a member type of a union type node in the type tree. */
  def visitUnionMember(using
      cache: StatementsCache
  )(
      tpe: cache.quotes.reflect.TypeRepr,
      annotations: Set[AnnotationInfo],
      isCollectionItem: Boolean,
      context: Context,
      visitNode: TypeTreeTermlessIterator.VisitNodeFunction
  ): Unit =
    given cache.quotes.type = cache.quotes
    visitSumCase(tpe, TypeNameUtils.typeNameOf(tpe), annotations, isCollectionItem, context, visitNode)

  /** Before visiting a union type node in the type tree. */
  def beforeUnion(using
      cache: StatementsCache
  )(
      tpe: cache.quotes.reflect.TypeRepr,
      annotations: Set[AnnotationInfo],
      context: Context
  ): Context =
    beforeSum(tpe, annotations, context)

  /** After visiting a union type node in the type tree. */
  def afterUnion(using
      cache: StatementsCache
  )(
      tpe: cache.quotes.reflect.TypeRepr,
      context: Context
  ): Unit =
    afterSum(tpe, context)

  /** Before visiting a Java Iterable node in the type tree. */
  def beforeJavaIterable(using
      cache: StatementsCache
  )(
      tpe: cache.quotes.reflect.TypeRepr,
      itemTpe: cache.quotes.reflect.TypeRepr,
      annotations: Set[AnnotationInfo],
      context: Context
  ): Context =
    beforeCollection(tpe, itemTpe, annotations, context)

  /** Visit an item of a Java Iterable node in the type tree. */
  def visitJavaIterableItem(using
      cache: StatementsCache
  )(
      tpe: cache.quotes.reflect.TypeRepr,
      annotations: Set[AnnotationInfo],
      context: Context,
      visitNode: TypeTreeTermlessIterator.VisitNodeFunction
  ): Unit =
    visitCollectionItem(tpe, annotations, context, visitNode)

  /** After visiting a Java Iterable node in the type tree. */
  def afterJavaIterable(using
      cache: StatementsCache
  )(
      tpe: cache.quotes.reflect.TypeRepr,
      context: Context
  ): Unit =
    afterCollection(tpe, context)

  /** Before visiting a Map node in the type tree. */
  def beforeMap(using
      cache: StatementsCache
  )(
      tpe: cache.quotes.reflect.TypeRepr,
      keyTpe: cache.quotes.reflect.TypeRepr,
      valueTpe: cache.quotes.reflect.TypeRepr,
      annotations: Set[AnnotationInfo],
      context: Context
  ): Context =
    beforeProduct(tpe, annotations, context)

  /** Visit an entry of a Map node in the type tree. */
  def visitMapEntry(using
      cache: StatementsCache
  )(
      tpe: cache.quotes.reflect.TypeRepr,
      annotations: Set[AnnotationInfo],
      context: Context,
      visitNode: TypeTreeTermlessIterator.VisitNodeFunction
  ): Unit =
    visitProductField(tpe, TagName("<key>"), annotations, context, visitNode)

  /** After visiting a Map node in the type tree. */
  def afterMap(using
      cache: StatementsCache
  )(
      tpe: cache.quotes.reflect.TypeRepr,
      context: Context
  ): Unit =
    afterProduct(tpe, context)

  /** Before visiting a Java Map node in the type tree. */
  def beforeJavaMap(using
      cache: StatementsCache
  )(
      tpe: cache.quotes.reflect.TypeRepr,
      keyTpe: cache.quotes.reflect.TypeRepr,
      valueTpe: cache.quotes.reflect.TypeRepr,
      annotations: Set[AnnotationInfo],
      context: Context
  ): Context =
    beforeMap(tpe, keyTpe, valueTpe, annotations, context)

  /** Visit an entry of a Java Map node in the type tree. */
  def visitJavaMapEntry(using
      cache: StatementsCache
  )(
      tpe: cache.quotes.reflect.TypeRepr,
      annotations: Set[AnnotationInfo],
      context: Context,
      visitNode: TypeTreeTermlessIterator.VisitNodeFunction
  ): Unit =
    visitMapEntry(tpe, annotations, context, visitNode)

  /** After visiting a Java Map node in the type tree. */
  def afterJavaMap(using
      cache: StatementsCache
  )(
      tpe: cache.quotes.reflect.TypeRepr,
      context: Context
  ): Unit =
    afterMap(tpe, context)

  /** Before visiting a Java Record node in the type tree. */
  def beforeJavaRecord(using
      cache: StatementsCache
  )(
      tpe: cache.quotes.reflect.TypeRepr,
      annotations: Set[AnnotationInfo],
      context: Context
  ): Context =
    beforeProduct(tpe, annotations, context)

  /** Visit a field of a Java Record node in the type tree. */
  def visitJavaRecordField(using
      cache: StatementsCache
  )(
      tpe: cache.quotes.reflect.TypeRepr,
      name: String,
      annotations: Set[AnnotationInfo],
      context: Context,
      visitNode: TypeTreeTermlessIterator.VisitNodeFunction
  ): Unit =
    visitProductField(tpe, name, annotations, context, visitNode)

  /** After visiting a Java Record node in the type tree. */
  def afterJavaRecord(using
      cache: StatementsCache
  )(
      tpe: cache.quotes.reflect.TypeRepr,
      context: Context
  ): Unit =
    afterProduct(tpe, context)
}
