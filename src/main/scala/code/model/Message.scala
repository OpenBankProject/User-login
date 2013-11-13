package code.model

import net.liftmodules.amqp.AMQPSender
import com.rabbitmq.client.{ConnectionFactory,Channel}

trait BankAccount{}

case class AddBankAccount(val id: String, val accountNumber : String, val blzIban : String, val pinCode : String) extends BankAccount
case class UpdateBankAccount(val id: String, val accountNumber : String, val blzIban : String, val pinCode : String) extends BankAccount
case class DeleteBankAccount(val id: String, val accountNumber : String, val blzIban : String) extends BankAccount

trait Response{
  val id: String
  val message: String
}

case class SuccessResponse(val id: String, val message: String) extends Response
case class ErrorResponse(val id: String, val message: String) extends Response