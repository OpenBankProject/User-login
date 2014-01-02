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