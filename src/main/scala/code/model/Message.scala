package com.tesobe.model

import net.liftmodules.amqp.AMQPSender
import com.rabbitmq.client.{ConnectionFactory,Channel}

trait BankAccount{}

case class AddBankAccountCredentials(val id: String, val accountNumber : String, val bankNationalIdentifier : String, val pinCode : String) extends BankAccount
case class UpdateBankAccountCredentials(val id: String, val accountNumber : String, val bankNationalIdentifier : String, val pinCode : String) extends BankAccount
case class DeleteBankAccountCredentials(val id: String, val accountNumber : String, val bankNationalIdentifier : String) extends BankAccount

trait Response{
  val id: String
  val message: String
}

case class SuccessResponse(val id: String, val message: String) extends Response
case class ErrorResponse(val id: String, val message: String) extends Response