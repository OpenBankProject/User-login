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
package code.util

import com.rabbitmq.client.ConnectionFactory
import net.liftmodules.amqp.{AMQPSender,StringAMQPSender,AMQPMessage}
import scala.actors._
import code.model.{BankAccount, BankAccountAMQPSender}

/**
 * An Example of how to use the Example subclass of AMQPSender[T]. Still following?
 */
object BankAccountSender {
  val factory = new ConnectionFactory {
    import ConnectionFactory._

    setHost(DEFAULT_HOST)
    setPort(DEFAULT_AMQP_PORT)
    setUsername(DEFAULT_USER)
    setPassword(DEFAULT_PASS)
    setVirtualHost(DEFAULT_VHOST)
  }

  //BankAccountAMQPSender(ConnectionFactory, EXCHANGE, QUEUE_ROUTING_KEY)
  val amqp = new BankAccountAMQPSender(factory, "directExchange", "management")

  def sendMessage(message: BankAccount) = {
     amqp ! AMQPMessage(message)
  }
}