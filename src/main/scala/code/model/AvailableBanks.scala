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
 package code.util

import net.liftweb.common.Loggable
import net.liftweb.util.Helpers.tryo
import net.liftweb.util.Props
import scala.io.Source

object GermanBanks extends Loggable{
  // key: bank name, value BLZ (bank identifier)
  private var availableBanks: Map[String,String] = Map()

  def getAvaliableBanks()= {
    if(availableBanks.isEmpty){
      for{
        path <- Props.get("banks.germany")
        source <- tryo{Source.fromFile(path, "iso-8859-1")}
      }yield{
        val allLines = source.getLines
        while(allLines.hasNext){
          val firstSplit = allLines.next.split("=")
          val secondSplit = firstSplit(1).split('|')
          //we add only the banks which have the important data in the file
          //HBCI URL and port
          if(
              secondSplit.length == 8 &&
              secondSplit(5).nonEmpty &&
              secondSplit(7).nonEmpty
            ){
            availableBanks += ((secondSplit(0),firstSplit(0)))
          }
        }
      }
      availableBanks
    }
    else
      availableBanks
  }
}