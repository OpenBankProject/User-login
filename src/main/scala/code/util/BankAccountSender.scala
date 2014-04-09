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
import net.liftmodules.amqp.{AMQPSender,StringAMQPSender,AMQPMessage}
import com.tesobe.model.BankAccount
import net.liftweb.util._
import net.liftweb.common.Loggable

object BankAccountSender extends Loggable{
  val factory = new ConnectionFactory {
    import ConnectionFactory._
    setHost(Props.get("connection.host", "localhost"))
    setPort(DEFAULT_AMQP_PORT)
    setUsername(Props.get("connection.user", DEFAULT_USER))
    setPassword(Props.get("connection.password", DEFAULT_PASS))
    setVirtualHost(DEFAULT_VHOST)
  }

  //BankAccountAMQPSender(ConnectionFactory, EXCHANGE, QUEUE_ROUTING_KEY)
  val amqp = new BankAccountAMQPSender(factory, "directExchange", "management")

  def sendMessage(message: BankAccount) = {
    logger.info(s"sending to the data storage: $message")
    amqp ! AMQPMessage(message)
  }
}

class BankAccountAMQPSender(cf: ConnectionFactory, exchange: String, routingKey: String)
 extends AMQPSender[BankAccount](cf, exchange, routingKey) {
  override def configure(channel: Channel) = {
    val conn = cf.newConnection()
    val channel = conn.createChannel()
    channel
  }
}