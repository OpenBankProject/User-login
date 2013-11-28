package code.model

case class HBCIBank(
  val name: String,
  val hbci_url: String,
  val hbci_port: String
)

case class SupportedBank(
  val name: String,
  val national_identifier: String
)

case class SupportedBanks(
  val banks: List[SupportedBank])