package ScalablyTyped
package NodeLib.StreamModule.internalNamespace

import scala.scalajs.js
import scala.scalajs.js.`|`
import scala.scalajs.js.annotation._

@js.native
trait Writable
  extends NodeLib.NodeJSNamespace.WritableStream
     with Stream {
  def end(str: java.lang.String): scala.Unit = js.native
  def end(str: java.lang.String, encoding: java.lang.String): scala.Unit = js.native
  def end(str: java.lang.String, encoding: java.lang.String, cb: js.Function): scala.Unit = js.native
}

