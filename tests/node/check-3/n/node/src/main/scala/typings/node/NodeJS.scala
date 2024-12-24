package typings.node

import org.scalablytyped.runtime.StObject
import scala.scalajs.js
import scala.scalajs.js.annotation.{JSGlobalScope, JSGlobal, JSImport, JSName, JSBracketAccess}

object NodeJS {
  
  // Polyfill for TS 5.6's instrinsic BuiltinIteratorReturn type, required for DOM-compatible iterators
  /** NOTE: Conditional type definitions are impossible to translate to Scala.
    * See https://www.typescriptlang.org/docs/handbook/2/conditional-types.html for an intro.
    * You'll have to cast your way around this structure, unfortunately.
    * TS definition: {{{
    std.ReturnType<std.Array<any>[symbol]> extends std.Iterator<any, infer TReturn, undefined> ? TReturn : any
    }}}
    */
  @js.native
  trait BuiltinIteratorReturn extends StObject
}
