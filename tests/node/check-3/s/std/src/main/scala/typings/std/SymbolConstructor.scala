package typings.std

import org.scalablytyped.runtime.StObject
import scala.scalajs.js
import scala.scalajs.js.annotation.{JSGlobalScope, JSGlobal, JSImport, JSName, JSBracketAccess}

// from lib.es2015.iterable.d.ts
trait SymbolConstructor extends StObject {
  
  val iterator: js.Symbol
}
object SymbolConstructor {
  
  inline def apply(iterator: js.Symbol): SymbolConstructor = {
    val __obj = js.Dynamic.literal(iterator = iterator.asInstanceOf[js.Any])
    __obj.asInstanceOf[SymbolConstructor]
  }
  
  @scala.inline
  implicit open class MutableBuilder[Self <: SymbolConstructor] (val x: Self) extends AnyVal {
    
    inline def setIterator(value: js.Symbol): Self = StObject.set(x, "iterator", value.asInstanceOf[js.Any])
  }
}
