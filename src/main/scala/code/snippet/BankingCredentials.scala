package code.snippet

import net.liftweb.http.{S,SHtml,RequestVar}
import net.liftweb.util._
import Helpers._
import net.liftweb.common.{Full, Failure, Empty, Box}
import scala.xml.NodeSeq
import scala.util.{Either, Right, Left}

import code.model.dataAccess.OBPUser

class BankingCrendetials{

  case class FormField[T](
    defaultValue: T,
    //the message in case the field value is not valid
    errorMessage: String,
    //the id of the error notice in the template where to show the error message
    errorNodeId: String
  ) extends RequestVar[T](defaultValue){
    def validate : Either[(String, String),Unit] =
      if(this.is == defaultValue)
        Left((this.errorNodeId, errorMessage))
      else
        Right()
  }
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

  //TODO: user scala macros / reflection to populate this list automatically ?
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

  private def processData() = {
    //TODO: encrypt the pin code, send a message in the message queue
  }

  private def checkInputs()= {
    val errors = validate(fields)
    if(errors.isEmpty){
      processData()
      //TODO generate the verifier, redirect /show it.
    }
    else{
      errors.foreach{
        e => S.error(e._1, e._2)
      }
    }
  }

  def render = {
    //TODO: check the access token is still valid
    OBPUser.currentUser match {
      case Full(user) => {
        val countries  = defaultCountry :: "Germany" :: Nil
        val banks = defaultBank :: Nil //TODO: fill me with real data

        "form [action]" #> {S.uri}&
        "#countrySelect"  #>
          SHtml.selectElem(countries,Full(country.is))(
            (v : String) => country.set(v)
          ) &
        "#bankSelect"  #>
          SHtml.selectElem(banks,Full(banks.head))(
            (v : String) => bank.set(v)
          ) &
        "#accountNumber" #> SHtml.textElem(accountNumber,("placeholder","123456")) &
        "#accountPin" #> SHtml.passwordElem(accountPin,("placeholder","******")) &
        "type=submit" #> SHtml.onSubmitUnit(checkInputs)

      }
      case _ => {
        S.error("loginError", "you need to be logged in to see the form")
        "#credentialsForm" #> NodeSeq.Empty
      }
    }
  }
}