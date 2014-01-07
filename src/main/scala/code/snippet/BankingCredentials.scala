package code.snippet

import net.liftweb.http.{S,SHtml,RequestVar}
import net.liftweb.http.js.{JsCmd, JsCmds}
import JsCmds._
import net.liftweb.http.js.jquery.JqJsCmds._
import net.liftweb.util._
import Helpers._
import net.liftweb.common.{Full, Failure, Empty, Box}
import scala.xml.NodeSeq
import scala.util.{Either, Right, Left}

import code.model.dataAccess.APIUser
import code.util.{RequestToken, GermanBanks, BankAccountSender, User}
import code.model.Token
import code.pgp.PgpEncryption
import com.tesobe.model.AddBankAccountCredentials


class BankingCrendetials{

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

  private def processData(t: Token, u: APIUser): String = {
    val id = randomString(8)
    val publicKey = Props.get("publicKeyPath").getOrElse("")
    val encryptedPin =
      PgpEncryption.encryptToString(accountPin.is, publicKey)
    val accountOwner = u.id_
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

  private def updatePage(messageId: String)= {
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

    val future: LAFuture[Response] = new LAFuture()
    // watingActor waits for acknowledgment if the bank account was added
    object waitingActor extends LiftActor{
      def messageHandler = {
        case r: Response => {
          future.complete(Full(r))
        }
      }
    }
    new ResponseAMQPListener(waitingActor, messageId)
    future onSuccess{response =>
      response match {
        case _: SuccessResponse => S.notice("info", response.message)
        case _: ErrorResponse => S.notice("info", response.message)
      }
    }

    future.onFail {
      case Failure(msg, _, _) => S.notice("info", msg)
      case _ => S.notice("info", "error please try later")
    }
  }

  private def renderForm(t: Token, u: APIUser) = {

    /**
    *
    */
    def processInputs(): JsCmd = {
      val errors = validate(fields)
      if(errors.isEmpty){
        val messageId = processData(t,u)
        updatePage(messageId)
        //TODO: redirect in case of success with the token
        //show an error message other wise
        Noop
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
    "#accountNumber" #> SHtml.textElem(accountNumber,("placeholder","123456")) &
    "#accountPin" #> SHtml.passwordElem(accountPin,("placeholder","******")) &
    "#processSubmit" #> SHtml.hidden(processInputs)
  }

  def render = {
    RequestToken.is match {
      case Full(token) if(token.isValid) =>
        User.is match {
          case Full(user) => {
            renderForm(token, user)
          }
          case _ => {
            S.error("loginError", "you need to be logged in to see the form")
            "#credentialsForm" #> NodeSeq.Empty
          }
        }
      case Full(token) => {
        S.error("loginError", "Token expired")
        "#credentialsForm" #> NodeSeq.Empty
      }
      case _ =>{
        S.error("loginError", "Token not found")
        "#credentialsForm" #> NodeSeq.Empty
      }
    }
  }
}