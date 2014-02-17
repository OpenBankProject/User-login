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

import net.liftweb.common.{Full, Box, Empty, Failure, Logger}
import net.liftweb.http.S
import net.liftweb.util.{Helpers, CssSel}
import scala.xml.NodeSeq
import net.liftweb.util.Helpers._
import net.liftweb.mapper.By
import java.net.URL
import net.liftweb.http.js.JsCmds.RedirectTo
import Helpers.tryo

import code.model.dataAccess.APIUser
import code.model.Token
import code.model.{RequestToken, CurrentUser}

class OAuthCallback extends Logger{
  private val NOOP_SELECTOR = "#i_am_an_id_that_should_never_exist" #> ""

  private def getURL(url: String): Box[URL] = {
    tryo{new URL(url)}
  }

  def redirectToBankingCredentials:CssSel = {
    if(S.post_?){
      (for{
        tokenParam <- S.param("token") ?~ "token parameter is missing"
        userIdParam <- S.param("user_id") ?~ "user_id parameter is missing"
        token <- getToken(tokenParam)
        url <- getAuthenticationURL(token)
        host <- getHost(url)
      } yield{
        val user = getOrCreateAPIUser(userIdParam, host)
        setSessionVars(user, token)
      }) match {
        case Full(a) => S.redirectTo("../banking-credentials")
        case Failure(msg, _, _) => S.error("error", msg)
        case _ => S.error("error", "could not register user.")
      }
      NOOP_SELECTOR
    }
    else{
      S.error("error", "no POST request found")
      NOOP_SELECTOR
    }
  }

  private def getToken(token: String): Box[Token] = {
    Token.find(By(Token.thirdPartyApplicationSecret, token)) match {
      case Full(token) => {
        if(token.isValid)
          Full(token)
        else
          Failure("token expired")
      }
      case _ =>
        Failure("token not found")
    }
  }

  private def getAuthenticationURL(token: Token): Box[String] = {
    token.consumerId.obj match {
      case Full(consumer) =>
        Full(consumer.userAuthenticationURL.get)
      case _ =>
        Failure("consumer not found.")
    }
  }

  private def getHost(authenticationURL: String) : Box[String] ={
    if(authenticationURL.nonEmpty){
      getURL(authenticationURL) match {
        case Full(url) =>{
          val host = url.getHost
          Full(host)
        }
        case _ =>
          Failure("non valid authentication URL. Could not create the User.")
      }
    }
    else
      Failure("authentication URL is empty. Could not create the User.")
  }

  private def setSessionVars(user: APIUser, token: Token): Unit = {
    CurrentUser.set(Full(user))
    RequestToken.set(Full(token))
  }

  def getOrCreateAPIUser(userId: String, host: String): APIUser = {
    APIUser.find(By(APIUser.providerId, userId), By(APIUser.provider_, host)) match {
      case Full(u) => {
        // logger.info("user exist already")
        u
      }
      case _ => {
        // logger.info("creating user")
        APIUser.create
        .provider_(host)
        .providerId(userId)
        .saveMe
      }
    }
  }
}