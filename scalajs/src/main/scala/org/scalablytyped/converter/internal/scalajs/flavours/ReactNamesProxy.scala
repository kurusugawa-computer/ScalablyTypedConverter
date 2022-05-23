package org.scalablytyped.converter.internal
package scalajs
package flavours

class ReactNamesProxy(reactNames: ReactNames, rewrites: IArray[CastConversion]) {
  private def withRewritten(qname: QualifiedName): List[QualifiedName] =
    rewrites.find(_.from === qname) match {
      case Some(rewrite) => List(qname, rewrite.to)
      case None          => List(qname)
    }

  val JsxIntrinsicElements: QualifiedName =
    reactNames.JsxIntrinsicElements

  val ComponentQNames: Set[QualifiedName] =
    reactNames.ComponentQNames.flatMap(withRewritten)

  val WrappedComponentsQNames: Set[QualifiedName] =
    reactNames.WrappedComponentsQNames.flatMap(withRewritten)

  val isRef: Set[QualifiedName] =
    reactNames.isRef.flatMap(withRewritten)

  def isComponent(tr: TypeRef): Option[PropsRef] =
    tr match {
      case TypeRef.Intersection(types, _) =>
        types.firstDefined(isComponent)
      case TypeRef(c, IArray.first(props), _) if ComponentQNames(c) =>
        isComponent(props).orElse(Some(unpackedProps(props)))
      case TypeRef(c, IArray.first(cc), _) if WrappedComponentsQNames(c) =>
        isComponent(cc)
      case _ =>
        None
    }

  /* the portion of the type system implemented in ST is not powerful enough to handle these constructs */
  def unpackedProps(propsRef: TypeRef): PropsRef =
    if (reactNames.PropsWithWithoutRefQNames(propsRef.typeName)) PropsRef(propsRef.targs.head)
    else if (reactNames.ComponentPropsWithWithoutRefQNames(propsRef.typeName)) PropsRef(propsRef.targs.head.targs.head)
    else PropsRef(propsRef)

  val isElement: Set[QualifiedName] =
    Set(reactNames.ReactElement, reactNames.JsxReactElement).flatMap(withRewritten)

  val isNode: Set[QualifiedName] =
    withRewritten(reactNames.ReactNode).toSet

  val isElementOrNode: Set[QualifiedName] =
    isElement ++ isNode
}
