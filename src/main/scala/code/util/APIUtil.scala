package code.util

import net.liftweb.http.JsonResponse
import net.liftweb.json.Extraction
import net.liftweb.json.JsonAST.JValue
import net.liftweb.http.js.JsExp
import net.liftweb.common.Full
import net.liftweb.util.Helpers._
import net.liftweb.http.S
import net.liftweb.http.js.JE.JsRaw
import scala.collection.JavaConversions.asScalaSet

object APIUtil {

  implicit val formats = net.liftweb.json.DefaultFormats
  implicit def errorToJson(error: ErrorMessage): JValue = Extraction.decompose(error)
  val headers = ("Access-Control-Allow-Origin","*") :: Nil

  case class ErrorMessage(
    error: String
  )

  case class SuccessMessage(
    success: String
  )

  case class BankAccountJSON(
    account_number: String,
    blz_iban: String,
    pin_code: String
  )

  case class PinCodeJSON(
    pin_code: String
  )


  def httpMethod : String =
    S.request match {
      case Full(r) => r.request.method
      case _ => "GET"
    }

  def isThereAnOAuthHeader : Boolean = {
    S.request match {
      case Full(a) =>  a.header("Authorization") match {
        case Full(parameters) => parameters.contains("OAuth")
        case _ => false
      }
      case _ => false
    }
  }

  def noContentJsonResponse : JsonResponse =
    JsonResponse(JsRaw(""), headers, Nil, 204)

  def successJsonResponse(message : String = "success", httpCode : Int = 200) : JsonResponse =
    JsonResponse(Extraction.decompose(SuccessMessage(message)), headers, Nil, httpCode)

  def errorJsonResponse(message : String = "error", httpCode : Int = 400) : JsonResponse =
    JsonResponse(Extraction.decompose(ErrorMessage(message)), headers, Nil, httpCode)

  def oauthHeaderRequiredJsonResponce : JsonResponse =
    JsonResponse(Extraction.decompose(ErrorMessage("Authentication via OAuth is required")), headers, Nil, 400)
}
