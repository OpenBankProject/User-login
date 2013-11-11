package code.util

// import com.rabbitmq.client.{ConnectionFactory,ConnectionParameters,Channel}
import com.rabbitmq.client.{ConnectionFactory,Channel}
import net.liftmodules.amqp.{AMQPAddListener,AMQPMessage, AMQPDispatcher, SerializedConsumer}
import scala.actors._
import net.liftweb.actor._
import net.liftweb.common.{Full,Box,Empty}
import net.liftweb.mapper.By
import net.liftweb.util.Helpers.tryo

class ResponseSerializedAMQPDispatcher[T](factory: ConnectionFactory)
    extends AMQPDispatcher[T](factory) {
  override def configure(channel: Channel) {
    val autoAck = false;
    channel.exchangeDeclare("directExchange2", "direct", false)
    channel.queueDeclare("response", false, false, false, null)
    channel.queueBind ("response", "directExchange2", "response")
    channel.basicConsume("response", autoAck, new SerializedConsumer(channel, this))
  }
}


object ResponseAMQPListener {
  lazy val factory = new ConnectionFactory {
    import ConnectionFactory._
    // localhost is a machine on your network with rabbitmq listening on port 5672
    setHost("localhost")
    setPort(DEFAULT_AMQP_PORT)
    setUsername(DEFAULT_USER)
    setPassword(DEFAULT_PASS)
    setVirtualHost(DEFAULT_VHOST)
  }

  val amqp = new ResponseSerializedAMQPDispatcher[String](factory) // string

  val stringListener = new LiftActor {
    protected def messageHandler = {
      case msg@AMQPMessage(contents: String) => println("received: " + msg)
    }
  }

  def startListen = {
    amqp ! AMQPAddListener(stringListener)
  }
}

