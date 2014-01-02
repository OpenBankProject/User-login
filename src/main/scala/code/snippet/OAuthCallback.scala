package code.snippet

import net.liftweb.common.{Full, Box, Empty}
import net.liftweb.http.S
import net.liftweb.util.{Helpers, CssSel}
import scala.xml.NodeSeq
import net.liftweb.util.Helpers._
import net.liftweb.mapper.By

import code.model.Token
import code.model.Token._

class OAuthCallback{
  def foo:CssSel = {
    if(S.post_?){
      // val token = S.param("token").getOrElse("no token")
      val token = S.param("token") match {
        case Full(tok) => {
          Token.find(By(Token.thirdPartyApplicationSecret, tok)) match {
            case Full(foundTok) =>{
              if (foundTok.isValid){
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


      val user_id = S.param("user_id") match {
        case Full(user) => user
        case _ => "no user id found in post request"
      }
      "#user_id * " #> {"User ID: "+user_id} & "#token * " #> {"Token: "+token}
    }
    else{
      "p * " #> "Post request not successful."
    }
  }
}