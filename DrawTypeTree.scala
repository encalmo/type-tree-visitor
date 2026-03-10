package org.encalmo.utils

import scala.quoted.Quotes
import scala.quoted.Expr
import scala.quoted.Type
import org.encalmo.utils.AnnotationUtils.AnnotationInfo
import scala.collection.mutable.Buffer

/** Draw the tree of a type as a string. */
object DrawTypeTree {

  private inline def debugMode = false

  inline def draw[T]: String = {
    ${ drawImpl[T] }
  }

  private def drawImpl[T: Type](using Quotes): Expr[String] = {
    import cache.quotes.reflect.*
    given cache: StatementsCache = new StatementsCache
    drawUsingTypeTreeIterator(using cache)(TypeRepr.of[T])
    cache.asExprOf[String]
  }

  private def drawUsingTypeTreeIterator(using
      cache: StatementsCache
  )(
      tpe: cache.quotes.reflect.TypeRepr
  ): Unit = {
    val context = DrawTypeTreeMacroContext(Buffer.empty[(Int, String)], 0, None)
    val trace = scala.collection.mutable.Buffer.empty[String]

    TypeTreeTermlessIterator.visitNode(using cache, DrawTypeTreeMacroVisitor)(
      tpe = tpe,
      context = context,
      isCollectionItem = false,
      annotations = Set.empty,
      trace = trace,
      debugIndent = if debugMode then 0 else Int.MinValue
    )

    import cache.quotes.reflect.*
    val tree = AsciiTree.draw(context.buffer.toList)
    println(tree)
    cache.put(Literal(StringConstant(tree)))

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

case class DrawTypeTreeMacroContext(buffer: Buffer[(Int, String)], indent: Int, fieldName: Option[String]) {
  def addNode(s: String): Unit = {
    buffer.append((indent, s))
  }
}

object DrawTypeTreeMacroVisitor extends SimpleTypeTreeTermlessVisitor {

  type Context = DrawTypeTreeMacroContext

  inline override def beforeNode(using
      cache: StatementsCache
  )(
      tpe: cache.quotes.reflect.TypeRepr,
      annotations: Set[AnnotationInfo],
      isCollectionItem: Boolean,
      context: Context
  ): (Context, Set[AnnotationInfo]) =
    given cache.quotes.type = cache.quotes
    context.addNode(
      context.fieldName.map(name => name + ": ").getOrElse("")
        + TypeNameUtils.typeNameOf(tpe)
    )
    (context.copy(indent = context.indent + 1, fieldName = None), annotations)

  inline override def visitProductField(using
      cache: StatementsCache
  )(
      tpe: cache.quotes.reflect.TypeRepr,
      name: TagName,
      annotations: Set[AnnotationInfo],
      context: Context,
      visitNode: TypeTreeTermlessIterator.VisitNodeFunction
  ): Unit = {
    visitNode(using cache, this)(
      tpe = tpe,
      annotations = annotations,
      isCollectionItem = false,
      context = context.copy(fieldName = Some(name.show(using cache)))
    )
  }

}

object AsciiTree {

  type Node = (Int, String)
  type Tree = List[Node]

  private val middleNode = "├── "
  private val endNode = "└── "
  private val link = "│   "
  private val space = " " * link.length

  /** Draw a tree */
  final def draw(pathsTree: Tree): String = {

    def drawLine(node: String, label: String, marks: List[Int]): (String, List[Int]) =
      marks match {
        case 0 :: Nil => (label, marks)
        case _ => ((0 until marks.max).map(i => if (marks.contains(i)) link else space).mkString + node + label, marks)
      }

    def draw2(label: String, ls: (List[Int], String)): (String, List[Int]) =
      ls._1 match {
        case 0 :: Nil => (label, ls._1)
        case _ => ((0 until ls._1.max).map(i => if (ls._1.contains(i)) link else space).mkString + ls._2 + label, ls._1)
      }

    def append(lineWithMarks: (String, List[Int]), result: String): (String, List[Int]) =
      (trimRight(lineWithMarks._1) + "\n" + result, lineWithMarks._2)

    pathsTree.reverse
      .foldLeft(("", List.empty[Int])) { case ((result, marks), (offset, label)) =>
        marks match {
          case Nil => drawLine(endNode, label, offset :: Nil)
          case head :: tail =>
            append(
              if (offset == head) drawLine(middleNode, label, marks)
              else if (offset < head)
                draw2(
                  label,
                  tail match {
                    case Nil                   => (offset :: Nil, endNode)
                    case x :: _ if x == offset => (tail, middleNode)
                    case _                     => (offset :: tail, endNode)
                  }
                )
              else {
                val l1 = drawLine(endNode, label, offset :: marks)
                val l2 = drawLine(space, "", offset :: marks)
                (l1._1 + "\n" + l2._1, l1._2)
              },
              result
            )
        }
      }
      ._1
  }

  private inline def trimRight(string: String): String = string.reverse.dropWhile(_ == ' ').reverse
}
