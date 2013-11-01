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
package code.model.dataAccess

import code.model._
import net.liftweb.common.{ Box, Full, Failure }
import net.liftweb.util.Helpers.tryo
import net.liftweb.json.JsonDSL._
import net.liftweb.common.Loggable
import net.liftweb.mapper.By


object LocalStorage extends MongoDBLocalStorage

trait LocalStorage extends Loggable {

  def getUser(id : String) : Box[User]
  def getCurrentUser : Box[User]
}

class MongoDBLocalStorage extends LocalStorage {

  def getUser(id : String) : Box[User] =
    OBPUser.find(By(OBPUser.email,id)) match {
      case Full(u) => Full(u)
      case _ => Failure("user " + id + " not found")
    }

  def getCurrentUser : Box[User] = OBPUser.currentUser
}