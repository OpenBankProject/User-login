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
import code.model.{BankAccountDetails, BankAccountDetailsAMQPSender}


object BankAccountDetailsSender {
  val factory = new ConnectionFactory()
  factory.setUsername("guest")
  factory.setPassword("guest")
  factory.setVirtualHost("/")
  factory.setRequestedHeartbeat(0)
  val connection = factory.newConnection()
  val channel = connection.createChannel();

  //here to declare consumer  for management
  channel.exchangeDeclare("directExchange", "direct", false)
  channel.queueDeclare("management", false, false, false, null)
  //                  QUEUE,         EXCHANGE,      QUEUE_ROUTING_KEY
  channel.queueBind ("management", "directExchange", "management")

  //BankAccountDetailsAMQPSender(ConnectionFactory, EXCHANGE, QUEUE_ROUTING_KEY)
  val amqp = new BankAccountDetailsAMQPSender(factory, "directExchange", "management")

  def sendMessage(message: BankAccountDetails) = {
     amqp ! AMQPMessage(message)
  }
}