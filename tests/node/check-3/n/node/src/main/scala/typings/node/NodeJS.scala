package typings.node

import org.scalablytyped.runtime.StObject
import scala.scalajs.js
import scala.scalajs.js.annotation.{JSGlobalScope, JSGlobal, JSImport, JSName, JSBracketAccess}

object NodeJS {
  
  // Polyfill for TS 5.6's instrinsic BuiltinIteratorReturn type, required for DOM-compatible iterators
  /** NOTE: Conditional type definitions are impossible to translate to Scala.
    * See https://www.typescriptlang.org/docs/handbook/2/conditional-types.html for an intro.
    * This RHS of the type alias is guess work. You should cast if it's not correct in your case.
    * TS definition: {{{
    std.ReturnType<std.Array<any>[symbol]> extends / * import warning: transforms.QualifyReferences#resolveTypeRef many Couldn't qualify globalThis.Iterator<any, infer TReturn> * / any ? TReturn : any
    }}}
    */
  type BuiltinIteratorReturn = TReturn
}
