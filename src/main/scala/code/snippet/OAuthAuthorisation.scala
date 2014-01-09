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
import net.liftweb.common.{Full, Box, Empty}
import net.liftweb.http.S
import code.model.{Consumer, Token}
import net.liftweb.mapper.By
import java.util.Date
import net.liftweb.util.Helpers
import code.model.AppType._
import code.model.TokenType
import TokenType._
import code.model.dataAccess.OBPUser
import code.model.CurrentUser
import scala.xml.NodeSeq
import net.liftweb.util.Helpers._
import code.model.RequestToken

object OAuthAuthorisation {

  private def shouldNotLogUserOut() : Boolean = {
    S.param("logUserOut") match {
      case Full("false") => true
      case _ => false
    }
  }

  private def showLoginForm(consumer: Box[Consumer], appToken: Token) = {
    def showForm(c: Consumer) = {

      S.param("failedLogin") match {
        case Full("true") =>
          S.error("tokenError", "Incorrect username or password")
        case _ =>
      }

      "#applicationName" #> c.name & {
        ".signup * " #> S.??("sign.up") &
        ".forgot *" #> S.??("lost.password") &
        ".submit [value]" #> S.??("log.in") &
        ".login [action]" #> OBPUser.loginPageURL &
        ".forgot [href]" #> {
          val href = for {
            menu <- OBPUser.lostPasswordMenuLoc
          } yield menu.loc.calcDefaultHref
          href getOrElse "#"
        } &
          ".signup [href]" #>
            OBPUser.signUpPath.foldLeft("")(_ + "/" + _)
      }
    }
    consumer match {
      case Full(c) => showForm(c)
      case _ => Consumer.find(By(Consumer.id, appToken.consumerId)) match {
        case Full(c) => showForm(c)
        case _ =>{
          S.error("tokenError","application not found")
          "#loginForm" #> NodeSeq.Empty
        }
      }
    }
  }

  /**
  *  if the delegate parameter is set to true, then get the application
  *  redirection URL, generate a token and redirect the user
  *  else show the login form
  */
  private def redirectOrShowLoginForm(delegate: Box[String], appToken: Token) = {
    delegate match {
      case Full("true") => {
        //get the redirection URL
        appToken.consumerId.obj match {
          case Full(c) => {
            val url = c.userAuthenticationURL.get
            if(url.nonEmpty){
              //generate a temporary token
              val secret = appToken.generateThirdPartyApplicationSecret
              val newURL = Helpers.appendParams(url, Seq(("token",secret)))
              //redirect the user with that token
              S.redirectTo(newURL)
              "#loginForm" #> NodeSeq.Empty
            }
            else{
              //show warning message
              S.error(
                "tokenError",
                "authentication URL empty, could not redirect."
              )
              //show the normal login form (we can't redirect)
              showLoginForm(Full(c), appToken)
            }
          }
          case _ =>{
            //show warning message
            S.error(
              "tokenError",
              "application not found"
            )
            "#loginForm" #> NodeSeq.Empty
          }
        }
      }
      case _ =>
        /*
        * the user will authenticate in this page
        * if logged in redirect to the banking credentials page
        * else show the login form
        */
        if (OBPUser.loggedIn_? && shouldNotLogUserOut()) {
          RequestToken.set(Full(appToken))
          CurrentUser.set(OBPUser.currentUser.get.user)
          S.redirectTo("/banking-credentials")
        }
        else {
          val currentUrl = S.uriAndQueryString.getOrElse("/")
          if(OBPUser.loggedIn_?) {
            OBPUser.logUserOut()
            //Bit of a hack here, but for reasons I haven't had time to discover,
            //if this page doesn't get refreshed here the session vars
            //OBPUser.loginRedirect and OBPUser.failedLoginRedirect don't
            //get set properly and the redirect after login gets messed up. -E.S.
            S.redirectTo(currentUrl)
          }
          //if login succeeds, reload the page with logUserOut=false to process it
          OBPUser.loginRedirect.set(
            Full(
              Helpers.appendParams(
                currentUrl, List(("logUserOut", "false"))
              )
            )
          )

          //if login fails, just reload the page with the login form visible
          OBPUser.failedLoginRedirect.set(
            Full(
              Helpers.appendParams(
                currentUrl, List(("failedLogin", "true"))
              )
            )
          )
          //the user is not logged in so we show a login form
          showLoginForm(Empty, appToken)
        }
    }
  }

  def tokenCheck = {
    S.param("oauth_token") match {
      case Full(token) => {
        Token.getRequestToken(Helpers.urlDecode(token.toString)) match {
          case Full(appToken) =>{
            //check if the token is still valid
            if (appToken.isValid) {
              //check what is the value of the redirection variable
              redirectOrShowLoginForm(
                S.param("delegate-authentication"),
                appToken
              )
            }
            else{
              S.error("tokenError", "Token expired")
              "#loginForm" #> NodeSeq.Empty
            }
          }
          case _ => {
            S.error("tokenError", {"This token "+token+" does not exist"})
            "#loginForm" #> NodeSeq.Empty
          }
        }
      }
      case _ =>{
        S.error("tokenError", "there is not token")
        "#loginForm" #> NodeSeq.Empty
      }
    }
  }
}