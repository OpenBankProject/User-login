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

import com.rabbitmq.client.{ConnectionFactory,Channel}
import net.liftmodules.amqp.{AMQPSender,AMQPMessage}
import com.tesobe.model.AddBankAccountCredentials
import net.liftweb.util.Props
import net.liftweb.common.Loggable

object MessageSender extends Loggable{
  import com.tesobe.status.model.SupportedBanksReply
  val factory = new ConnectionFactory {
    import ConnectionFactory._
    setHost(Props.get("connection.host", "localhost"))
    setPort(DEFAULT_AMQP_PORT)
    setUsername(Props.get("connection.user", DEFAULT_USER))
    setPassword(Props.get("connection.password", DEFAULT_PASS))
    setVirtualHost(DEFAULT_VHOST)
  }

  val amqp = new MessageSender[AddBankAccountCredentials](factory, "directExchange", "management")
  val amqp2 = new MessageSender[SupportedBanksReply](factory, "banksListResponse", "banksList")

  def sendBankingCredentials(message: AddBankAccountCredentials) = {
    logger.info(s"sending to the data storage: $message")
    amqp ! AMQPMessage(message)
  }

  def sendBanksList(message: SupportedBanksReply) = {
    logger.info(s"sending to the status application: $message")
    amqp2 ! AMQPMessage(message)
  }
}

class MessageSender[T](cf: ConnectionFactory, exchange: String, routingKey: String)
 extends AMQPSender[T](cf, exchange, routingKey) {
  override def configure(channel: Channel) = {
    val conn = cf.newConnection()
    val channel = conn.createChannel()
    channel
  }
}