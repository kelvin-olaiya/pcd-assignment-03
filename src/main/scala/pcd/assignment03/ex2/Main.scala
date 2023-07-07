package pcd.assignment03.ex2

import com.google.gson.Gson
import com.rabbitmq.client.AMQP.BasicProperties
import com.rabbitmq.client.impl.AMQImpl.Connection
import com.rabbitmq.client.{AMQP, Channel, ConnectionFactory, Consumer, DeliverCallback, Delivery}
import pcd.assignment03.ex2.pixelart.{Brush, BrushManager, PixelGrid, PixelGridView}

import java.awt.{Color, Label, PopupMenu}
import java.util.UUID
import java.util.concurrent.{CompletableFuture, TimeUnit, TimeoutException}
import javax.swing.{JButton, JFrame, JPanel, WindowConstants}
import scala.reflect.ClassTag
import scala.quoted.Type
import scala.util.Random

object CommunicationConfig:
  val channel: Channel =
    val factory = ConnectionFactory()
    factory.setHost("2.tcp.eu.ngrok.io")
    factory.setPort(16481)
    factory.newConnection().createChannel()

  val COLOR_CHANGE_EXCHANGE: String = "color_change"
  val PIXEL_COLOR_EXCHANGE: String = "pixel_color"
  val MOUSE_MOVE_EXCHANGE: String = "mouse_move"
  val USER_EXIT_EXCHANGE: String = "user_exit"

  val STATE_REQUEST_QUEUE: String = "state_request"


  Seq(
    COLOR_CHANGE_EXCHANGE,
    PIXEL_COLOR_EXCHANGE,
    MOUSE_MOVE_EXCHANGE,
    USER_EXIT_EXCHANGE,
  ).foreach(declareExchange)

  private def declareExchange(exchangeName: String) =
    channel.exchangeDeclare(exchangeName, "fanout")

  private def declareQueue(queueName: String) =
    channel.queueDeclare(queueName, false, false, false, null)

  val gson = Gson()

  def setDeliverCallback(exchangeName: String, deliverCallback: DeliverCallback): Any =
    val eventQueue: String = channel.queueDeclare.getQueue
    channel.queueBind(eventQueue, exchangeName, "")
    channel.basicConsume(eventQueue, true, deliverCallback, _ => {})

  extension (delivery: Delivery) {
    private def asString = String(delivery.getBody, "UTF-8")
    def unmarshall[T](cls: Class[T]): T = gson.fromJson(delivery.asString, cls)
  }

  def publishToExchange(message: Message, exchangeName: String): Unit =
    val marshalled = gson.toJson(message)
    channel.basicPublish(exchangeName, "", null, marshalled.getBytes("UTF-8"))

  def publishToQueue(message: Message, queueName: String, props: AMQP.BasicProperties = null): Unit =
    val marshalled = gson.toJson(message)
    channel.basicPublish("", queueName, props, marshalled.getBytes("UTF-8"))


enum Message extends Serializable:
  case PixelColor(x: Int, y: Int, UUID: UUID)
  case ColorChange(color: Int, UUID: UUID)
  case MouseMove(x: Int, y: Int, UUID: UUID)
  case UserExit(UUID: UUID)
  case StateRequest()
  case StateReply(grid: PixelGrid, brushes: Map[UUID, Brush])

object Main extends App:
  import CommunicationConfig.*
  import Message.*

  type User = (UUID, Brush)
  val localUser = (UUID.randomUUID(), Brush(0,0, Random.nextInt(10000)))
  var users = Map(localUser)
  var brushManager = new BrushManager()
  brushManager.addBrush(localUser._2)

  def requestGrid(): PixelGrid =
    var grid = PixelGrid(40, 40)
    publishToExchange(ColorChange(localUser._2.getColor, localUser._1), COLOR_CHANGE_EXCHANGE)
    val responseQueue: String = channel.queueDeclare.getQueue
    val props = BasicProperties().builder()
      .replyTo(responseQueue)
      .build()
    publishToQueue(StateRequest(), STATE_REQUEST_QUEUE, props)
    val future = CompletableFuture[StateReply]()
    val ctag = channel.basicConsume(responseQueue, true, (consumerTag, delivery) => {
      val message = delivery.unmarshall(classOf[StateReply])
      future.complete(message)
    }, _ => {})
    try {
      val stateToBe = future.get(5, TimeUnit.SECONDS)
      // TODO: create and set grid
    } catch {
      case _: TimeoutException => println("Timeout occurred, starting blank")
    } finally {
      channel.basicCancel(ctag)
    }
    grid
  val grid = requestGrid()
  val view = new PixelGridView(requestGrid(), brushManager, 800, 800)

  // CLICK
  view.addPixelGridEventListener((x: Int, y: Int) => {
    publishToExchange(PixelColor(x, y, localUser._1), PIXEL_COLOR_EXCHANGE)
  })

  // COLOR CHANGE
  view.addColorChangedListener(color => {
    publishToExchange(ColorChange(color, localUser._1), COLOR_CHANGE_EXCHANGE)
  })

  // MOUSE MOVE
  view.addMouseMovedListener((x: Int, y: Int) => {
    publishToExchange(MouseMove(x, y, localUser._1), MOUSE_MOVE_EXCHANGE)
    localUser._2.updatePosition(x, y)
    view.refresh()
  })

  setDeliverCallback(PIXEL_COLOR_EXCHANGE, (consumerTag, delivery) => {
    val message = delivery.unmarshall(classOf[PixelColor])
    println(s" [x] Pixel color received $message")
    grid.set(message.x, message.y, users(message.UUID).getColor)
    view.refresh()
  })

  setDeliverCallback(COLOR_CHANGE_EXCHANGE, (consumerTag, delivery) => {
    val message = delivery.unmarshall(classOf[ColorChange])
    println(s" [x] Color change received $message")
    if !(users.keySet contains message.UUID) then
      val otherBrush = Brush(0, 0, message.color)
      users = users + (message.UUID -> otherBrush)
      brushManager.addBrush(otherBrush)
    else
      users(message.UUID).setColor(message.color)
    view.refresh()
  })

  setDeliverCallback(MOUSE_MOVE_EXCHANGE, (consumerTag, delivery) => {
    val message = delivery.unmarshall(classOf[MouseMove])
    if (users.keySet contains message.UUID) && (message.UUID != localUser._1) then
      println(s" [x] Mouse move received $message")
      users(message.UUID).updatePosition(message.x, message.y)
      view.refresh()
  })

  channel.basicConsume(STATE_REQUEST_QUEUE, true, (consumerTag, delivery) => {
    val message = delivery.unmarshall(classOf[StateReply])
    println(message.grid)
  }, _ => {})

  // TODO: USER_EXIT_EXCHANGE

  view.display();

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

