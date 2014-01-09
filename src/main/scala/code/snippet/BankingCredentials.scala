 /**
Open Bank Project

Copyright 2011,2012 TESOBE / Music Pictures Ltd.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

 Open Bank Project (http://www.openbankproject.com)
      Copyright 2011,2012 TESOBE / Music Pictures Ltd

      This product includes software developed at
      TESOBE (http://www.tesobe.com/)
    by
    Ayoub Benali : ayoub AT tesobe Dot com
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

import code.model.dataAccess.OBPUser
import code.util.{RequestToken, GermanBanks, BankAccountSender}
import code.model.Token
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
  private val defaultCountry = "Choose a country"
  private val defaultBank = "Choose a bank"

  private object country extends FormField[String](
    defaultCountry,
    "Country not selected ! ",
    "countryError"
  )

  private object bank extends FormField[String](
    defaultBank,
    "Bank not selected ! ",
    "bankError"
  )
  private object accountNumber extends FormField[String](
    "",
    "Account number is Empty ! ",
    "accountNumberError"
  )

  private object accountPin extends FormField[String](
    "",
    "Account pin is Empty ! ",
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

  private def processData(t: Token, u: OBPUser): String = {
    val id = randomString(8)
    val publicKey = Props.get("publicKeyPath").getOrElse("")
    val encryptedPin =
      PgpEncryption.encryptToString(accountPin.is, publicKey)
    val accountOwner = u.user.obj.map(_.id_).getOrElse("")
    val message =
      AddBankAccountCredentials(
        id,
        accountNumber,
        bank,
        encryptedPin,
        accountOwner
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
      case _ => Failure("Could not save banking credentials. Please try later.")
    }

  }

  private def generateVerifier(token: Token, user: OBPUser) :Box[String] = {
    if (token.verifier.isEmpty) {
      val randomVerifier = token.gernerateVerifier
      token.userId(user.user.obj.map{_.id_}.getOrElse(""))
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

  private def renderForm(token: Token, user: OBPUser) = {

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
                  JsHideId("errorMessage") &
                  JsHideId("account") &
                  SetHtml("verifier",Unparsed(v))
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
                SetHtml("error", Unparsed("error, please try later"))
              }
            }
          }
          case Failure(msg,_, _) => {
            //show an error message other wise
            SetHtml("error", Unparsed(msg))
          }
          case _ => {
            SetHtml("error", Unparsed("error, please try later"))
          }
        }
      }
      else{
        errors.foreach{
          e => S.error(e._1, e._2)
        }
        Noop
      }
    }

    val countries  = defaultCountry :: "Germany" :: Nil
    val availableBanks = GermanBanks.getAvaliableBanks()
    val banks: Seq[String] = availableBanks.keySet.toSeq :+ defaultBank

    "form [action]" #> {S.uri}&
    "#countrySelect"  #>
      SHtml.selectElem(countries,Full(country.is))(
        (v : String) => country.set(v)
      ) &
    "#bankSelect" #>
      //TODO: change the default case to be the latest value rather than head
      SHtml.selectElem(banks,Full(banks.head))(
        (v : String) => bank.set(availableBanks(v))
      ) &
    "#accountNumber" #> SHtml.textElem(accountNumber,("placeholder","123456789")) &
    "#accountPin" #> SHtml.passwordElem(accountPin,("placeholder","***********")) &
    "#processSubmit" #> SHtml.hidden(processInputs)
  }


  def render = {
    def hideCredentialsForm() = {
      "#credentialsForm" #> NodeSeq.Empty
    }

    RequestToken.is match {
      case Full(token) if(token.isValid) =>
        OBPUser.currentUser match {
          case Full(user) => {
            renderForm(token, user)
          }
          case _ => {
            S.error("error", "you need to be logged in to see the form")
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