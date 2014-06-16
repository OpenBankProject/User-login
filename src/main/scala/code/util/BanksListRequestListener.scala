/**
Open Bank Project - API
Copyright (C) 2011, 2014, TESOBE / Music Pictures Ltd

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
package code.util

import net.liftmodules.amqp.AMQPDispatcher
import com.rabbitmq.client.ConnectionFactory
import net.liftweb.actor._
import net.liftweb.common.Loggable
import net.liftweb.util.Props

// an AMQP dispatcher that waits for message coming from a specif queue
// and dispatching them to the subscribed actors

class BanksListDispatcher[T](factory: ConnectionFactory)
    extends AMQPDispatcher[T](factory) {
  import com.rabbitmq.client.Channel

  override def configure(channel: Channel) {
    import net.liftmodules.amqp.SerializedConsumer

    channel.exchangeDeclare("getBanks", "direct", false)
    channel.queueDeclare("banksRequest", false, false, false, null)
    channel.queueBind("banksRequest", "getBanks", "banksRequest")
    channel.basicConsume("banksRequest", false, new SerializedConsumer(channel, this))
  }
}

object BanksListListener extends Loggable{
  import net.liftmodules.amqp.AMQPAddListener
  import com.tesobe.status.model.{GetSupportedBanks, SupportedBanksReply, BankInfo}
  import code.model.GermanBanks

  lazy val factory = new ConnectionFactory {
    import ConnectionFactory._
    setHost(Props.get("connection.host", "localhost"))
    setPort(DEFAULT_AMQP_PORT)
    setUsername(Props.get("connection.user", DEFAULT_USER))
    setPassword(Props.get("connection.password", DEFAULT_PASS))
    setVirtualHost(DEFAULT_VHOST)
  }

  val amqp = new BanksListDispatcher[GetSupportedBanks](factory)

  object requestHandler extends LiftActor with Loggable {
    import net.liftmodules.amqp.AMQPMessage

    protected def messageHandler = {
      case msg@AMQPMessage(statues: GetSupportedBanks) => {
        logger.info("received banks list request message")
        // compute it and send the message to the actor responding
        val message =
          SupportedBanksReply(
            GermanBanks
            .getAvaliableBanks
            .map{
              case (bankId, bankDetails) => {
                BankInfo("DEU", bankId, bankDetails.name)
              }
            }
            .toSet
          )
        MessageSender.sendBanksList(message)
      }
    }
  }

  def startListen = {
    logger.info("started listening for banks list request")
    amqp ! AMQPAddListener(requestHandler)
  }
}
