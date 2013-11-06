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
  Simon Redfern : simon AT tesobe DOT com
  Stefan Bethge : stefan AT tesobe DOT com
  Everett Sochowski : everett AT tesobe DOT com
  Ayoub Benali: ayoub AT tesobe DOT com

 */
package code.api

import net.liftweb.http.JsonResponse
import net.liftweb.http.rest._
import net.liftweb.json.Printer._
import net.liftweb.json.Extraction
import net.liftweb.json.JsonAST._
import net.liftweb.common.{Failure,Full,Empty, Box, Loggable}
import _root_.net.liftweb.util._
import _root_.net.liftweb.util.Helpers._
import code.model._
import code.util.APIUtil._
import net.liftweb.json
import code.util.BankAccountSender
import code.model.{AddBankAccount, UpdateBankAccount, DeleteBankAccount}
import net.liftmodules.amqp.AMQPMessage
import code.pgp.PgpEncryption

object BankaccountsManagement extends OBPRestHelper with Loggable {

  implicit def errorToJson(error: ErrorMessage): JValue = Extraction.decompose(error)

  // TODO: nicer prefix
  val apiPrefix = "obp" / "v1" oPrefix _


  oauthServe(apiPrefix {
    case "bankaccounts" :: Nil JsonPost jsonBody -> _ => {
      user =>
        for {
          bankAccountJson <- tryo{jsonBody.extract[BankAccountJSON]} ?~ "wrong JSON format"
          publicKey <- Props.get("publicKeyPath")
          u <- user ?~ "user not found"
        } yield {
          val encrypted_pin = PgpEncryption.encryptToString(bankAccountJson.pin_code, publicKey)
          val message = AddBankAccount(bankAccountJson.account_number, bankAccountJson.blz_iban, encrypted_pin)
          BankAccountSender.sendMessage(message)
          successJsonResponse(Extraction.decompose(message) , 201)
        }
    }
  })

  oauthServe(apiPrefix{
    case "bankaccounts" :: blz_iban :: account_number :: Nil JsonPut jsonBody -> _ => {
      user =>
        for {
          bankAccountJson <- tryo{jsonBody.extract[PinCodeJSON]} ?~ "wrong JSON format"
          publicKey <- Props.get("publicKeyPath")
          u <- user ?~ "user not found"
        } yield {
          val encrypted_pin = PgpEncryption.encryptToString(bankAccountJson.pin_code, publicKey)
          val message = UpdateBankAccount(account_number, blz_iban, encrypted_pin)
          BankAccountSender.sendMessage(message)
          successJsonResponse(Extraction.decompose(message) , 201)
        }
    }
  })

  oauthServe(apiPrefix {
    case "bankaccounts" :: blz_iban :: account_number :: Nil JsonDelete jsonBody => {
      user =>
        for {
          publicKey <- Props.get("publicKeyPath") //just for having sth between brackets when user commented
          u <- user ?~ "user not found"
        } yield {
          val message = DeleteBankAccount(account_number, blz_iban)
          BankAccountSender.sendMessage(message)
          // successJsonResponse(Extraction.decompose(message) , 201)
          noContentJsonResponse
        }
    }
  })
}
