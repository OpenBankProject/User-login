package code.model

import scala.actors._
import net.liftmodules.amqp.AMQPSender
import com.rabbitmq.client.{ConnectionFactory,Channel}

trait BankAccountDetails{}

case class AddBankAccount(accountNumber : String, blzIban : String, pinCode : String) extends BankAccountDetails
case class UpdateBankAccount(accountNumber : String, blzIban : String, pinCode : String) extends BankAccountDetails
case class DeleteBankAccount(accountNumber : String, blzIban : String) extends BankAccountDetails

class BankAccountDetailsAMQPSender(cf: ConnectionFactory, exchange: String, routingKey: String)
 extends AMQPSender[BankAccountDetails](cf, exchange, routingKey) {
  override def configure(channel: Channel) = {
    val conn = cf.newConnection()
    val channel = conn.createChannel()
    channel
  }
}