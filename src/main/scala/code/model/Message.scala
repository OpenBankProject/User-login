package code.model

import scala.actors._
import net.liftmodules.amqp.AMQPSender
import com.rabbitmq.client.{ConnectionFactory,Channel}

trait BankAccount{}

case class AddBankAccount(accountNumber : String, blzIban : String, pinCode : String) extends BankAccount
case class UpdateBankAccount(accountNumber : String, blzIban : String, pinCode : String) extends BankAccount
case class DeleteBankAccount(accountNumber : String, blzIban : String) extends BankAccount