package pcd.assignment03.ex2

import com.rabbitmq.client.impl.AMQImpl.Connection
import com.rabbitmq.client.{Channel, ConnectionFactory, Consumer, DeliverCallback, Delivery}
import pcd.assignment03.ex2.pixelart.BrushManager

import java.awt.{Label, PopupMenu}
import javax.swing.{JButton, JFrame, JPanel, WindowConstants}

object CommunicationConfig:
  val channel: Channel =
    val factory = ConnectionFactory()
    factory.newConnection().createChannel()

  val COLOR_CHANGE_EXCHANGE: String = "color_change"
  val PIXEL_COLOR_EXCHANGE: String = "pixel_color"
  val MOUSE_MOVE_EXCHANGE: String = "mouse_move"
  val USER_EXIT_EXCHANGE: String = "user_exit"

  Seq(
    COLOR_CHANGE_EXCHANGE,
    PIXEL_COLOR_EXCHANGE,
    MOUSE_MOVE_EXCHANGE,
    USER_EXIT_EXCHANGE
  ).foreach(declareExchange)

  private def declareExchange(exchangeName: String) =
    channel.exchangeDeclare(exchangeName, "fanout")

  def setDeliverCallback(exchangeName: String, deliverCallback: DeliverCallback) =
    val eventQueue: String = channel.queueDeclare.getQueue
    channel.queueBind(eventQueue, exchangeName, "")
    channel.basicConsume(eventQueue, true, deliverCallback, _ => {})

  // def publishToExchange(exchangeName: String, ???)

object Main extends App:
  import CommunicationConfig.*
  setDeliverCallback(MOUSE_MOVE_EXCHANGE, (consumerTag, delivery) => {
    val message = new String(delivery.getBody, "UTF-8")
    System.out.println(" [x] Received '" + message + "'")
  })
  channel.basicPublish(MOUSE_MOVE_EXCHANGE, "", null, "wow".getBytes("UTF-8"))

  // CONNECT to BROKER
  // CREATE/DECLARE EXCHANGES and QUEUES
  //  - exchange events "events" --> {PixelColoring, MouseMoved, ColorChanged, UserJoined} type: fanout
  //  - queue getState(props: replyTo) "get_state" type: direct
  //    - queue replyTo => ad_hoc --- rpc pattern
  // REQUEST GRID:
  //  IF timeout occurs -> enable editing
  //  WHILE waiting BUFFER received events
  // ON grid establishment APPLY buffered events
  // REGISTER event listeners

