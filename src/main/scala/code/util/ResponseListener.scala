package code.util

import com.rabbitmq.client.{ConnectionFactory,Channel, DefaultConsumer, Envelope, BasicProperties, AMQP}
import net.liftmodules.amqp.{AMQPAddListener,AMQPMessage, AMQPDispatcher}
import net.liftweb.actor._
import net.liftweb.common.{Full,Box,Empty}
import net.liftweb.mapper.By
import net.liftweb.util.Helpers.tryo
import net.liftweb.util._
import java.io.ObjectInputStream
import java.io.ByteArrayInputStream
import com.tesobe.model.Response



class ResponseSerializedAMQPDispatcher[T](factory: ConnectionFactory, actor: LiftActor, messageId: String)
    extends AMQPDispatcher[T](factory) {
  override def configure(channel: Channel) {
    val autoAck = false;
    channel.exchangeDeclare("directExchange2", "direct", false)
    channel.queueDeclare("response", false, false, false, null)
    channel.queueBind ("response", "directExchange2", "response")
    channel.basicConsume("response", autoAck, new SerializedConsumer(channel, actor, messageId))
  }
}

//consumer checks if id of the saved message fits the id of the sent message
class SerializedConsumer(channel: Channel, a: LiftActor, id: String) extends DefaultConsumer(channel) {
  override def handleDelivery(tag: String, env: Envelope, props: AMQP.BasicProperties, body: Array[Byte]){
    val routingKey = env.getRoutingKey
    val contentType = props.getContentType
    val deliveryTag = env.getDeliveryTag
    val in = new ObjectInputStream(new ByteArrayInputStream(body))
    tryo{ in.readObject.asInstanceOf[Response] } match {
      case Full(t) =>
        if(id == t.id){
          a ! t
          channel.basicAck(deliveryTag, false);
          channel.close()
        }
      case _ =>
    }
  }
}

class ResponseAMQPListener(actor: LiftActor, messageId:String) {
  lazy val factory = new ConnectionFactory {
    import ConnectionFactory._
    setHost("localhost")
    setPort(DEFAULT_AMQP_PORT)
    setUsername(Props.get("connection.user", DEFAULT_USER))
    setPassword(Props.get("connection.password", DEFAULT_PASS))
    setVirtualHost(DEFAULT_VHOST)
  }

  new ResponseSerializedAMQPDispatcher[Response](factory, actor, messageId)

}

