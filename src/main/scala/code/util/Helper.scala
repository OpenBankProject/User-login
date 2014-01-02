package code.util

import net.liftweb.http.js.JsCmd

object Helper{
  case class JsHideByClass(className: String) extends JsCmd {
    def toJsCmd = s"$$('.$className').hide()"
  }
}