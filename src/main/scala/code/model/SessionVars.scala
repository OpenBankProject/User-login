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

 */
package code.model

import net.liftweb.http.SessionVar
import net.liftweb.common.{Box, Empty}
import code.model.dataAccess.APIUser

/**
* a request token singleton unique per session.
* The purpose is to share the request token between the pages without reloading
* each time the token form the database.
*
* Note: SessionVar is used rather that RequestVar because RequestVar is valid
* only for the same HTTP request (same page) while rendering different pages
* is responding to different HTTP requests.
*/
object RequestToken extends SessionVar[Box[Token]](Empty)

object CurrentUser extends SessionVar[Box[APIUser]](Empty)