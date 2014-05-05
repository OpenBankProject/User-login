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
package bootstrap.liftweb


import net.liftweb._
import util._
import common._
import http._
import sitemap._
import Loc._
import mapper._
import code.model.dataAccess._
import code.model.{Consumer, Token}
import net.liftweb.util.Helpers._
import net.liftweb.json.JsonDSL._
import net.liftweb.util.Schedule
import net.liftweb.http.js.jquery.JqJsCmds
import net.liftweb.util.Helpers
import javax.mail.{ Authenticator, PasswordAuthentication }
import java.io.FileInputStream
import java.io.File
import code.util.Helper
import net.liftweb.http.js.JsCmds
import code.api.OAuthHandshake
import code.util.BanksListListener

/**
 * A class that's instantiated early and run.  It allows the application
 * to modify lift's environment
 */
class Boot extends Loggable{
  def boot {


    val contextPath = LiftRules.context.path
    val propsPath = tryo{Box.legacyNullTest(System.getProperty("props.resource.dir"))}.toIterable.flatten

    logger.info("external props folder: " + propsPath)

    /**
     * Where this application looks for props files:
     *
     * All properties files follow the standard lift naming scheme for order of preference (see https://www.assembla.com/wiki/show/liftweb/Properties)
     * within a directory.
     *
     * The first choice of directory is $props.resource.dir/CONTEXT_PATH where $props.resource.dir is the java option set via -Dprops.resource.dir=...
     * The second choice of directory is $props.resource.dir
     *
     * For example, on a production system:
     *
     * api1.example.com with context path /api1
     *
     * Looks first in (outside of war file): $props.resource.dir/api1, following the normal lift naming rules (e.g. production.default.props)
     * Looks second in (outside of war file): $props.resource.dir, following the normal lift naming rules (e.g. production.default.props)
     * Looks third in the war file
     *
     * and
     *
     * api2.example.com with context path /api2
     *
     * Looks first in (outside of war file): $props.resource.dir/api2 , following the normal lift naming rules (e.g. production.default.props)
     * Looks second in (outside of war file): $props.resource.dir, following the normal lift naming rules (e.g. production.default.props)
     * Looks third in the war file, following the normal lift naming rules
     *
     */

    val firstChoicePropsDir = for {
      propsPath <- propsPath
    } yield {
      Props.toTry.map {
        f => {
          val name = propsPath + contextPath + f() + "props"
          name -> { () => tryo{new FileInputStream(new File(name))} }
        }
      }
    }

    val secondChoicePropsDir = for {
      propsPath <- propsPath
    } yield {
      Props.toTry.map {
        f => {
          val name = propsPath +  f() + "props"
          name -> { () => tryo{new FileInputStream(new File(name))} }
        }
      }
    }

    Props.whereToLook = () => {
      firstChoicePropsDir.flatten.toList ::: secondChoicePropsDir.flatten.toList
    }


    if (!DB.jndiJdbcConnAvailable_?) {
      val driver =
        Props.mode match {
          case Props.RunModes.Production | Props.RunModes.Staging | Props.RunModes.Development =>  Props.get("db.driver") openOr "org.h2.Driver"
          case _ => "org.h2.Driver"
        }
      val vendor =
        Props.mode match {
          case Props.RunModes.Production | Props.RunModes.Staging | Props.RunModes.Development =>
            new StandardDBVendor(driver,
               Props.get("db.url") openOr "jdbc:h2:lift_proto.db;AUTO_SERVER=TRUE",
               Props.get("db.user"), Props.get("db.password"))
          case _ =>
            new StandardDBVendor(
              driver,
              "jdbc:h2:mem:OBPTest;DB_CLOSE_DELAY=-1",
              Empty, Empty)
        }

      logger.debug("Using database driver: " + driver)
      LiftRules.unloadHooks.append(vendor.closeAllConnections_! _)

      DB.defineConnectionManager(DefaultConnectionIdentifier, vendor)
    }

    val runningMode = Props.mode match {
      case Props.RunModes.Production => "Production mode"
      case Props.RunModes.Staging => "Staging mode"
      case Props.RunModes.Development => "Development mode"
      case Props.RunModes.Test => "test mode"
      case _ => "other mode"
    }

    logger.info("running mode: " + runningMode)

    // Use Lift's Mapper ORM to populate the database
    // you don't need to use Mapper to use Lift... use
    // any ORM you want
    Schemifier.schemify(true, Schemifier.infoF _, OBPUser)

    // where to search snippet
    LiftRules.addToPackages("code")

    //OAuth Mapper
    Schemifier.schemify(true, Schemifier.infoF _, Token)
    Schemifier.schemify(true, Schemifier.infoF _, Consumer)


    // Build SiteMap
    val sitemap = List(
          Menu.i("OAuth") / "oauth" / "authorize", //OAuth authorization page
          Menu.i("Callback") / "oauth" / "callback",
          Menu.i("Banking Credentials") / "banking-credentials"

    )

    def sitemapMutators = OBPUser.sitemapMutator

    // set the sitemap.  Note if you don't want access control for
    // each page, just comment this line out.
    LiftRules.setSiteMapFunc(() => sitemapMutators(SiteMap(sitemap : _*)))
    // Use jQuery 1.4
    LiftRules.jsArtifacts = net.liftweb.http.js.jquery.JQuery14Artifacts

    LiftRules.ajaxStart = Full( () => JsCmds.JsShowId("ajax-spinner") & Helper.JsHideByClass("hide-during-ajax") &  Helper.JsHideByClass("error"))
    LiftRules.ajaxEnd = Full( () => JsCmds.JsHideId("ajax-spinner") )

    // Force the request to be UTF-8
    LiftRules.early.append(_.setCharacterEncoding("UTF-8"))

    // What is the function to test if a user is logged in?
    LiftRules.loggedInTest = Full(() => OBPUser.loggedIn_?)

    // Use HTML5 for rendering
    LiftRules.htmlProperties.default.set((r: Req) =>
      new Html5Properties(r.userAgent))

    LiftRules.explicitlyParsedSuffixes = Helpers.knownSuffixes &~ (Set("com"))

    // Make a transaction span the whole HTTP request
    S.addAround(DB.buildLoanWrapper)

    LiftRules.statelessDispatchTable.append(OAuthHandshake)
    LiftRules.ajaxRetryCount = Full(0)
    LiftRules.ajaxPostTimeout = seconds(20) toInt

    BanksListListener.startListen
  }
}