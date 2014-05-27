/**
Open Bank Project - API
Copyright (C) 2011, 2013, TESOBE / Music Pictures Ltd

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.

Email: contact@tesobe.com
TESOBE / Music Pictures Ltd
Osloerstrasse 16/17
Berlin 13359, Germany

  This product includes software developed at
  TESOBE (http://www.tesobe.com/)
  by
  Ayoub Benali: ayoub AT tesobe DOT com

 */
package code.util

import net.liftweb.http.js.JsCmd
import net.liftweb.common.Loggable
import net.liftweb.util.{Mailer, Props}


object Helper{
  case class JsHideByClass(className: String) extends JsCmd {
    def toJsCmd = s"$$('.$className').hide()"
  }

  case class JsShowByClass(className: String) extends JsCmd {
    def toJsCmd = s"$$('.$className').show()"
  }
}

object MyExceptionLogger extends Loggable{
  import net.liftweb.http.Req

  def unapply(in: (Props.RunModes.Value, Req, Throwable)): Option[(Props.RunModes.Value, Req, Throwable)] = {
    import net.liftweb.util.Helpers.now
    import Mailer.{From, To, Subject, PlainMailBodyType}

    val outputStream = new java.io.ByteArrayOutputStream
    val printStream = new java.io.PrintStream(outputStream)
    in._3.printStackTrace(printStream)
    val currentTime = now.toString
    val stackTrace = new String(outputStream.toByteArray)
    val error = currentTime + ": " + stackTrace
    val host = Props.get("hostname", "unknown host")

    val mailSent = for {
      from <- Props.get("mail.exception.sender.address") ?~ "Could not send mail: Missing props param for 'from'"
      // no spaces, comma separated e.g. mail.api.consumer.registered.notification.addresses=notify@example.com,notify2@example.com,notify3@example.com
      toAddressesString <- Props.get("mail.exception.registered.notification.addresses") ?~ "Could not send mail: Missing props param for 'to'"
    } yield {

      //technically doesn't work for all valid email addresses so this will mess up if someone tries to send emails to "foo,bar"@example.com
      val to = toAddressesString.split(",").toList
      val toParams = to.map(To(_))
      val params = PlainMailBodyType(error) :: toParams

      //this is an async call
      Mailer.sendMail(
        From(from),
        Subject(s"you got an exception on $host"),
        params :_*
      )
    }

    //if Mailer.sendMail wasn't called (note: this actually isn't checking if the mail failed to send as that is being done asynchronously)
    if(mailSent.isEmpty)
      logger.warn(s"Exception notification failed: $mailSent")

     None
  }
}