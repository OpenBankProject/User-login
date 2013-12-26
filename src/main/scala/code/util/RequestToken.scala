package code.util

import net.liftweb.http.SessionVar
import net.liftweb.common.{Box, Empty}
import code.model.Token

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