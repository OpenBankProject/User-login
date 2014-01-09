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
  Nina Gänsdorfer: nina AT tesobe DOT com

 */
package code.snippet

import net.liftweb.http.{S,SHtml,RequestVar}
import net.liftweb.http.js.{JsCmd, JsCmds}
import JsCmds._
import net.liftweb.http.js.jquery.JqJsCmds._
import net.liftweb.http.LiftRules
import net.liftweb.util._
import Helpers._
import net.liftweb.common.{Full, Failure, Empty, Box, Loggable}
import scala.xml.{NodeSeq, Unparsed}
import scala.util.{Either, Right, Left}

import code.model.dataAccess.APIUser
import code.util.{BankAccountSender, Helper}
import code.model.{Token, RequestToken, CurrentUser, GermanBanks}
import code.pgp.PgpEncryption
import com.tesobe.model.AddBankAccountCredentials


class BankingCrendetials extends Loggable{

  class FormField[T](
    defaultValue: => T,
    //the message in case the field value is not valid
    errorMessage: => String,
    //the id of the error notice node (span/div) in the template where
    //to show the error message
    errorNodeId: => String
  ) extends RequestVar[T](defaultValue){
    /**
    * Left (the error case) contains the pair (error node Id,  error message)
    * Right Unit
    */
    def validate : Either[(String, String),Unit] =
      if(this.is == defaultValue)
        Left((this.errorNodeId, errorMessage))
      else
        Right()
  }

  //TODO: use locate for the text to make it available in different languages
  private val defaultCountry = S.??("choose_country")
  private val defaultBank = S.??("choose_bank")

  private object country extends FormField[String](
    defaultCountry,
     S.??("no_country_selected"),
    "countryError"
  )

  private object bank extends FormField[String](
    defaultBank,
    S.??("no_bank_selected"),
    "bankError"
  )
  private object accountNumber extends FormField[String](
    "",
    S.??("account_number_empty"),
    "accountNumberError"
  )

  private object accountPin extends FormField[String](
    "",
    S.??("account_pin_empty"),
    "accountPinError"
  )

  //TODO: user Scala macros / reflection to populate this list automatically ?
  private val fields =
    Seq(
    country,
    bank,
    accountNumber,
    accountPin
  )

  /**
  * go through the fields and collect the Left cases (error)
  */
  private def validate(fields: Seq[FormField[_]]) : Seq[(String,String)] = {
    fields
      .map{_.validate}
      .collect{
        case l: Left[(String,String), Unit] => l.a
      }
  }

  private def processData(t: Token, u: APIUser): String = {
    val id = randomString(8)
    val publicKey = Props.get("publicKeyPath").getOrElse("")
    val encryptedPin =
      PgpEncryption.encryptToString(accountPin.is, publicKey)
    val accountOwnerId = u.id_
    val accountOwnerProvider = u.provider
    val message =
      AddBankAccountCredentials(
        id,
        accountNumber,
        bank,
        encryptedPin,
        accountOwnerId,
        accountOwnerProvider
      )
    BankAccountSender.sendMessage(message)
    id
  }

  private def updatePage(messageId: String): Box[Unit] = {
    import net.liftweb.actor.{
      LAFuture,
      LiftActor
    }
    import com.tesobe.model.{
      Response,
      SuccessResponse,
      ErrorResponse
    }
    import code.util.ResponseAMQPListener

    val response: LAFuture[Response] = new LAFuture()
    // watingActor waits for acknowledgment if the bank account was added
    object waitingActor extends LiftActor{
      def messageHandler = {
        case r: Response => {
          response.complete(Full(r))
        }
      }
    }
    new ResponseAMQPListener(waitingActor, messageId)

    //TODO: change that to be asynchronous
    // we try to wait for the response for a amount time that is less than
    // lift ajax post timeout
    response.get(LiftRules.ajaxPostTimeout - 2000) match {
      case Full(r) =>
        r match {
          case _: SuccessResponse => {
            Full({})
          }
          case _: ErrorResponse =>{
           Failure(r.message)
          }
        }
      case _ => Failure("not_saved")
    }

  }

  private def generateVerifier(token: Token, user: APIUser) :Box[String] = {
    if (token.verifier.isEmpty) {
      val randomVerifier = token.gernerateVerifier
      token.userId(user.id_)
      if (token.save())
        Full(randomVerifier)
      else{
        logger.warn(s"Could not save token ${token.key}")
        Empty
      }
    }
    else
      Full(token.verifier)
  }

  private def renderForm(token: Token, user: APIUser) = {

    /**
    *
    */
    def processInputs(): JsCmd = {
      val errors = validate(fields)
      if(errors.isEmpty){
        updatePage(processData(token,user)) match {
          case Full(_) => {
            //redirect or show the verifier

            generateVerifier(token, user) match {
              case Full(v) => {
                if (token.callbackURL.is == "oob")
                  JsHideId("error") &
                  SetHtml("verifier",Unparsed(v)) &
                  JsShowId("verifierBloc") &
                  Helper.JsHideByClass("hide-during-ajax")
                else {
                  //redirect the user to the application with the verifier
                  val redirectionUrl =
                    Helpers.appendParams(
                      token.callbackURL,
                      Seq(("oauth_token", token.key),("oauth_verifier", v))
                    )
                  S.redirectTo(redirectionUrl)
                  Noop
                }
              }
              case _ => {
                SetHtml("error", Unparsed(S.??("error_try_later"))) &
                Helper.JsHideByClass("hide-during-ajax")
              }
            }
          }
          case Failure(msg,_, _) => {
            //show an error message other wise
            val error = SHtml.span(Unparsed(msg), Noop, ("class","error"))
            SetHtml("error", error) &
            Helper.JsHideByClass("hide-during-ajax")
          }
          case _ => {
            val error = <span class="error">S.??("error_try_later")</span>
            SetHtml("error",error) &
            Helper.JsHideByClass("hide-during-ajax")
          }
        }
      }
      else{
        errors.foreach{
          e => S.error(e._1, e._2)
        }
        Helper.JsShowByClass("hide-during-ajax")
      }
    }

    val countries  = defaultCountry :: S.??("germany") :: Nil
    val availableBanks = GermanBanks.getAvaliableBanks() map {
      case (bankname, bankId) => (s"$bankname ($bankId)", bankId)
    }
    val banks: Seq[String] =  Seq(defaultBank) ++ availableBanks.keySet.toSeq.sortWith(_.toLowerCase < _.toLowerCase)
    "form [action]" #> {S.uri}&
    "#countrySelect"  #>
      SHtml.selectElem(countries,Full(country.is))(
        (v : String) => country.set(v)
      ) &
    "#bankSelect" #>
      SHtml.selectElem(banks,Full(banks.head))(
        (bankname : String) => availableBanks.get(bankname) map {
          bankId => bank.set(bankId)
          }
      ) &
    "#accountNumber" #> SHtml.textElem(accountNumber,("placeholder","123456789")) &
    "#accountPin" #> SHtml.passwordElem(accountPin,("placeholder","***********")) &
    "#processSubmit" #> SHtml.hidden(processInputs) &
    "#saveBtn [value]" #> S.??("save")
  }

  def render = {
    def hideCredentialsForm() = {
      "#credentialsForm" #> NodeSeq.Empty
    }

    RequestToken.is match {
      case Full(token) if(token.isValid) =>
        CurrentUser.is match {
          case Full(user) => {
            renderForm(token, user)
          }
          case _ => {
            S.error("error", S.??("login_required"))
            hideCredentialsForm()
          }
        }
      case Full(token) => {
        S.error("error", "Token expired")
        hideCredentialsForm()
      }
      case _ =>{
        S.error("error", "Token not found")
        hideCredentialsForm()
      }
    }
  }
}