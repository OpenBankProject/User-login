package code.snippet

import net.liftweb.common.{Full, Box, Empty}
import net.liftweb.http.S
import net.liftweb.util.{Helpers, CssSel}
import scala.xml.NodeSeq
import net.liftweb.util.Helpers._
import net.liftweb.mapper.By
import java.net.URL
import net.liftweb.http.js.JsCmds.RedirectTo

import code.model.dataAccess.APIUser
import code.model.Token
import code.model.Token._
import code.util.{RequestToken, User}

class OAuthCallback{
  def foo:CssSel = {
    if(S.post_?){
      val token = S.param("token") match {
        case Full(tok) => {
          Token.find(By(Token.thirdPartyApplicationSecret, tok)) match {
            case Full(foundTok) => {
              if (foundTok.isValid){
                foundTok.consumerId.obj match {
                  case Full(consumer) => {
                    val url: URL = new URL(consumer.userAuthenticationURL)
                    val host = url.getHost

                    val user_id = S.param("user_id") match {
                      case Full(id) => {
                        createAPIUser(id, host, foundTok)
                        id
                      }
                      case _ => "no id found"
                    }

                  }
                  case _ => // do nothing
                }
                foundTok.thirdPartyApplicationSecret
              }
              else{
                "token not valid"
              }
            }
            case _ => "no token found in database"
          }
        }
        case _ => "no token found in post request"
      }

      S.param("user_id") match {
        case Full(user_id) => {
          "#user_id * " #> {"User ID: "+user_id} & "#token * " #> {"Token: "+token}
        }
        case _ => // "no user id found"
      }

      val user_id = S.param("user_id").getOrElse("no user id found in post request")
      "#user_id * " #> {"User ID: "+user_id} & "#token * " #> {"Token: "+token}
    }
    else{
      "#post_result * " #> "Post request not successful."
    }
  }

  def createAPIUser(user_id: String, host: String, token: Token) = {
   val user = APIUser.create
      .provider_(host)
      .providerId(user_id)
      // .saveMe

    User.set(Full(user))
    RequestToken.set(Full(token))
    S.redirectTo("../banking-credentials")
  }
}