package code.util

import net.liftweb.common.Loggable
import scala.io.{Source,Codec}

import code.model.HBCIBank

object BlzMapper extends Loggable{
  var availableBanks: Map[String,HBCIBank] = Map()

  def getAvaliableBanks(){
    val source = Source.fromFile("src/main/resources/props/blz.properties", "iso-8859-1")
    val allLines = source.getLines

    while(allLines.hasNext){
      val firstSplit = allLines.next.split("=")
      val secondSplit = firstSplit(1).split('|')
      if(secondSplit.length == 8){
        availableBanks += ((firstSplit(0), HBCIBank(secondSplit(0), secondSplit(5), secondSplit(7))))
      }
    }
    availableBanks
  }
}
