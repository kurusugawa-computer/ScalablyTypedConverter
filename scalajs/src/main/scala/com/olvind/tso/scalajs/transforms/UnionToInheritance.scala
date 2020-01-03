package com.olvind.tso
package scalajs
package transforms

import com.olvind.tso.seqs._

/**
  * Inheritance works better than the userland union types, so we rewrite the latter to the former:
  *
  * ```scala
  * package thisLib {
  *   trait Foo
  *   trait Bar
  *   type A = thisLib.Foo | thisLib.Bar
  * }
  *
  * // ->
  *
  * package thisLib {
  *   trait A
  *   trait Foo extends thisLib.A
  *   trait Bar extends thisLib.A
  * }
  * ```
  *
  * There is also a somewhat more complicated case where some types cannot be rewritten:
  * ```scala
  * package otherLib {
  *   trait FooBar
  * }
  * package thisLib {
  *   trait Foo
  *   trait Bar
  *   trait WithTypeParams[T]
  *   type A[T] = thisLib.Foo | thisLib.Bar | thisLib.WithTypeParams[T] | otherLib.FooBar
  * }
  *
  * // ->
  *
  * package thisLib {
  *   trait ABase
  *   trait Foo extends thisLib.ABase
  *   trait Bar extends thisLib.ABase
  *   trait WithTypeParams[T]
  *   type A[T] = ABase | WithTypeParams[T] | otherLib.FooBar
  * }
  * ```
  *
  * And finally, an even more complicated case where we invert type parameters, barring quite a few restrictions to
  *  (hopefully) keep the transformations sound:
  *
  * ```typescript
  * interface Either<L, R> {
  *     value: R
  * }
  * type Foo<T1, T2, T3> = "foo" | Either<T2, T3>
  * ```
  *
  * We turn this into
  * ```scala
  * trait Foo[T1, T2, T3] extends js.Object
  *
  * trait Either[L, R]
  *   extends Foo[js.Any, L, R] {
  *   var value: R
  * }
  *
  * sealed trait foo with Foo[js.Any, js.Any, js.Any]
  * ```
  */
object UnionToInheritance {
  final case class WasUnion(related: Seq[TypeRef]) extends Comment.Data

  def apply(scope: TreeScope, tree: ContainerTree, inLib: Name): ContainerTree = {
    val rewrites = Rewrite.identify(inLib, tree, scope / tree)

    val newParentsByCodePath: Map[QualifiedName, Seq[InvertingTypeParamRef]] =
      rewrites
        .flatMap(InvertingTypeParamRef.apply)
        .groupBy(_._1)
        .mapValues(_.map(_._2).sortBy(_.codePath.parts.last))

    val indexedRewrites: Map[QualifiedName, Rewrite] =
      rewrites.groupBy(_.original.codePath).mapValues(_.head)

    val withRewrittenTypes =
      typesToInterfaces(tree, indexedRewrites, newParentsByCodePath)

    addedInheritance(withRewrittenTypes, newParentsByCodePath)
  }

  def typesToInterfaces(
      c:                    ContainerTree,
      indexedRewrites:      Map[QualifiedName, Rewrite],
      newParentsByCodePath: Map[QualifiedName, Seq[InvertingTypeParamRef]],
  ): ContainerTree = {
    val newMembers = c.members.flatMap {
      case p: ContainerTree =>
        typesToInterfaces(p, indexedRewrites, newParentsByCodePath) :: Nil

      case ta: TypeAliasTree if indexedRewrites.contains(ta.codePath) =>
        val comment = ta.alias match {
          case TypeRef.Union(tps) =>
            val strings = tps.map(Printer.formatTypeRef(0)).map("  - " + _)
            Some(Comment(s"/* Rewritten from type alias, can be one of: \n${strings.mkString("\n")}\n*/\n"))
          case _ => None
        }

        indexedRewrites(ta.codePath) match {
          case Rewrite(_, asInheritance, Nil) =>
            val cls = ClassTree(
              List(Annotation.ScalaJSDefined),
              ta.name,
              ta.tparams,
              Nil,
              Nil,
              Nil,
              ClassType.Trait,
              isSealed = false,
              ta.comments +? comment + CommentData(KeepOnlyReferenced.Related(asInheritance)) +
                CommentData(WasUnion(asInheritance)),
              ta.codePath,
            )

            cls :: Nil
          case Rewrite(_, asInheritance, noRewrites) =>
            val patchedTa = patchCodePath(ta)
            val cls = ClassTree(
              List(Annotation.ScalaJSDefined),
              patchedTa.name,
              ta.tparams,
              newParentsByCodePath.getOrElse(ta.codePath, Nil).map(_.instantiate(ta.tparams)),
              Nil,
              Nil,
              ClassType.Trait,
              isSealed = false,
              Comments(
                List(CommentData(KeepOnlyReferenced.Related(asInheritance)), CommentData(WasUnion(asInheritance))),
              ),
              patchedTa.codePath,
            )
            val newTa = ta.copy(
              alias = TypeRef.Union(
                TypeRef(patchedTa.codePath, TypeParamTree.asTypeArgs(patchedTa.tparams), NoComments) +: noRewrites,
                sort = false,
              ),
              comments = ta.comments +? comment,
            )
            cls :: newTa :: Nil
        }
      case other => other :: Nil
    }

    c.withMembers(newMembers)
  }

  def patchCodePath(ta: TypeAliasTree): TypeAliasTree = {
    val newName = Name("_" + ta.name.unescaped)
    ta.copy(name = newName, codePath = QualifiedName(ta.codePath.parts.init :+ newName))
  }

  def addedInheritance(
      c:                    ContainerTree,
      newParentsByCodePath: Map[QualifiedName, Seq[InvertingTypeParamRef]],
  ): ContainerTree =
    c.withMembers(c.members.map {
      case p: PackageTree =>
        addedInheritance(p, newParentsByCodePath)
      case p: ModuleTree =>
        addedInheritance(p, newParentsByCodePath)
      case x: ClassTree =>
        newParentsByCodePath.get(x.codePath) match {
          case None => x
          case Some(newParents) =>
            x.copy(parents = x.parents ++ newParents.map(_.instantiate(x.tparams)))
        }
      case other => other
    })

  final case class Rewrite(original: TypeAliasTree, asInheritance: Seq[TypeRef], unchanged: Seq[TypeRef])

  object Rewrite {
    def identify(inLib: Name, p: ContainerTree, scope: TreeScope): Seq[Rewrite] = {
      def go(p: ContainerTree, scope: TreeScope): Seq[Rewrite] = {
        def legalClassName(name: Name): Boolean =
          p index name forall {
            case _: PackageTree => false
            case _ => true
          }
        p.members.flatMap {
          case p:  ContainerTree                            => identify(inLib, p, scope / p)
          case ta: TypeAliasTree if legalClassName(ta.name) => canRewrite(inLib, ta, scope / ta).toList
          case _ => Nil
        }
      }

      val all = go(p, scope)

      includeUnchangeds(all)
    }

    private def includeUnchangeds(all: Seq[Rewrite]): Seq[Rewrite] = {
      val allIndexed = all.groupBy(_.original.codePath).mapValues(_.head)

      def go(name: QualifiedName): Seq[TypeRef] =
        allIndexed.get(name) match {
          case None => Nil
          case Some(Rewrite(_, asInheritance, unchanged)) =>
            unchanged ++ asInheritance.flatMap(u => go(u.typeName))
        }

      all.map(r => r.copy(unchanged = r.unchanged ++ r.asInheritance.map(_.typeName).flatMap(go).distinct))
    }

    def canRewrite(inLib: Name, ta: TypeAliasTree, scope: TreeScope): Option[Rewrite] =
      ta.alias match {
        case TypeRef.Union(types) =>
          def legalTarget(tr: TypeRef): Boolean =
            scope.lookup(tr.typeName).exists {
              case (_:  ClassTree, _)     => true
              case (ta: TypeAliasTree, _) => canRewrite(inLib, ta, scope).isDefined
              case _ => false
            }

          val InLibrary: PartialFunction[TypeRef, TypeRef] = {
            case tr @ TypeRef(QualifiedName(_ :: `inLib` :: _), _, _) if legalTarget(tr) => tr
          }

          val HasIllegalTypeParams: PartialFunction[TypeRef, TypeRef] = {
            case tr @ TypeRef(_, targs, _)
                if targs.exists(x => !scope.isAbstract(x)) || targs.toSet.size =/= targs.size =>
              tr
          }

          types.partitionCollect2(HasIllegalTypeParams, InLibrary) match {
            case (_, inLibrary, _) if inLibrary.size <= 1 => None
            case (illegalTParams, inLibrary, outsideLib) =>
              Some(Rewrite(ta, inLibrary, illegalTParams ++ outsideLib))
          }

        case _ => None
      }
  }

  case class InvertingTypeParamRef(codePath: QualifiedName, tParamRefs: Seq[(TypeParamTree, Option[Int])]) {
    def instantiate(tparams: Seq[TypeParamTree]): TypeRef =
      TypeRef(
        codePath,
        tParamRefs.map {
          case (_, Some(idx)) => TypeRef(tparams(idx).name)
          case (_, None)      => TypeRef.Any
        }.toList,
        NoComments,
      )
  }

  object InvertingTypeParamRef {
    def apply(r: Rewrite): Seq[(QualifiedName, InvertingTypeParamRef)] = {
      val parentType: TypeAliasTree =
        if (r.unchanged.isEmpty) r.original else patchCodePath(r.original)

      r.asInheritance.map { (newParent: TypeRef) =>
        val tParamReferencedAt: Seq[(TypeParamTree, Option[Int])] =
          parentType.tparams.map(tparam =>
            tparam -> newParent.targs.zipWithIndex.collectFirst {
              case (x, idx) if x.name === tparam.name => idx
            },
          )
        newParent.typeName -> InvertingTypeParamRef(parentType.codePath, tParamReferencedAt)
      },
    }
  }
}
