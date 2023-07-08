package pcd.assignment03.ex2

import com.google.gson.{Gson, GsonBuilder, JsonDeserializationContext, JsonDeserializer, JsonElement, JsonObject, JsonPrimitive, JsonSerializationContext, JsonSerializer, TypeAdapter}
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.{JsonReader, JsonWriter}
import com.rabbitmq.client
import com.rabbitmq.client.AMQP.{BasicProperties, Queue}
import com.rabbitmq.client.impl.AMQImpl.Connection
import com.rabbitmq.client.{AMQP, Channel, ConnectionFactory, Consumer, DeliverCallback, Delivery}
import pcd.assignment03.ex2.MapTypeAdapter
import pcd.assignment03.ex2.pixelart.{Brush, BrushManager, PixelGrid, PixelGridView}

import java.awt.event.{WindowAdapter, WindowEvent}
import java.awt.{Color, Label, PopupMenu}
import java.lang.reflect
import java.util.UUID
import java.util.concurrent.{CompletableFuture, TimeUnit, TimeoutException}
import javax.swing.{JButton, JFrame, JPanel, WindowConstants}
import scala.collection.concurrent.TrieMap
import scala.reflect.ClassTag
import scala.quoted.Type
import scala.util.Random

object CommunicationConfig:
  val connection: client.Connection =
    val factory = ConnectionFactory()
    factory.setHost("localhost") // 2.tcp.eu.ngrok.io
    // factory.setPort(16481)
    factory.newConnection()

  val channel: Channel =
    connection.createChannel()

  val COLOR_CHANGE_EXCHANGE: String = "color_change"
  val PIXEL_COLOR_EXCHANGE: String = "pixel_color"
  val MOUSE_MOVE_EXCHANGE: String = "mouse_move"
  val USER_EXIT_EXCHANGE: String = "user_exit"

  val STATE_REQUEST_QUEUE: String = "state_request"


object CommunicationApi:
  import Utils.*
  import CommunicationConfig.*

  private def declareExchange(exchangeName: String) =
    channel.exchangeDeclare(exchangeName, "fanout")

  def declareQueue(queueName: String): Queue.DeclareOk =
    channel.queueDeclare(queueName, false, false, false, null)

  def declareAllExchanges(exchanges: Iterable[String]): Unit =
    exchanges.foreach(declareExchange)

  def registerDeliveryCallback(exchangeName: String, deliverCallback: DeliverCallback): String =
    val eventQueue: String = channel.queueDeclare.getQueue
    channel.queueBind(eventQueue, exchangeName, "")
    channel.basicConsume(eventQueue, true, deliverCallback, _ => {})
    eventQueue

  def publishToExchange(message: Message, exchangeName: String): Unit =
    val marshalled = gson.toJson(message)
    channel.basicPublish(exchangeName, "", null, marshalled.getBytes("UTF-8"))

  def publishToQueue(message: Message, queueName: String, props: AMQP.BasicProperties = null): Unit =
    val marshalled = gson.toJson(message)
    channel.basicPublish("", queueName, props, marshalled.getBytes("UTF-8"))

object Utils:
  import CommunicationConfig.*
  import CommunicationApi.*
  import Message.*

  val gson: Gson = GsonBuilder()
    .registerTypeAdapter(classOf[TrieMap[UUID, Brush]], MapTypeAdapter)
    .create()

  extension (delivery: Delivery) {
    private def asString = String(delivery.getBody, "UTF-8")
    def unmarshall[T](cls: Class[T]): T = gson.fromJson(delivery.asString, cls)
  }

enum Message extends Serializable:
  case PixelColor(x: Int, y: Int, UUID: UUID)
  case ColorChange(color: Int, UUID: UUID)
  case MouseMove(x: Int, y: Int, UUID: UUID)
  case UserExit(UUID: UUID)
  case StateRequest()
  case StateReply(grid: PixelGrid, users: TrieMap[UUID, Brush])

object Main extends App:
  import CommunicationConfig.*
  import CommunicationApi.*
  import Utils.*
  import Message.*

  private def requestGrid(): PixelGrid =
    var grid = PixelGrid(40, 40)
    val responseQueue: String = channel.queueDeclare.getQueue
    val props = BasicProperties().builder()
      .replyTo(responseQueue)
      .build()
    println("Requesting grid status")
    publishToQueue(StateRequest(), STATE_REQUEST_QUEUE, props)
    val future = CompletableFuture[StateReply]()
    val cancellationTag = channel.basicConsume(responseQueue, true, (_, delivery) => {
      val message = delivery.unmarshall(classOf[StateReply])
      future.complete(message)
    }, _ => {})

    try {
      val result = future.get(5, TimeUnit.SECONDS)
      grid = result.grid
      users.addAll(result.users)
    } catch {
      case _: TimeoutException => println("Timeout occurred, starting with blank grid")
    } finally {
      channel.basicCancel(cancellationTag)
      channel.queueDelete(responseQueue)
    }
    grid

  declareAllExchanges(Seq(COLOR_CHANGE_EXCHANGE, PIXEL_COLOR_EXCHANGE, MOUSE_MOVE_EXCHANGE, USER_EXIT_EXCHANGE))
  declareQueue(STATE_REQUEST_QUEUE)

  var users = TrieMap[UUID, Brush]()
  var brushManager = new BrushManager()

  private val localUser = (UUID.randomUUID(), Brush(0,0, Random.nextInt(256 * 256 * 256)))
  val grid = requestGrid()
  users.addOne(localUser._1 -> localUser._2)
  users.foreach((_, b) => brushManager.addBrush(b))
  val view = new PixelGridView(grid, brushManager, 800, 800)

  publishToExchange(ColorChange(localUser._2.getColor, localUser._1), COLOR_CHANGE_EXCHANGE)

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

  private val pixelColorQueue = registerDeliveryCallback(PIXEL_COLOR_EXCHANGE, (_, delivery) => {
    val message = delivery.unmarshall(classOf[PixelColor])
    println(s" [x] Event received => $message")
    grid.set(message.x, message.y, users(message.UUID).getColor)
    view.refresh()
  })

  private val colorChangeQueue = registerDeliveryCallback(COLOR_CHANGE_EXCHANGE, (_, delivery) => {
    val message = delivery.unmarshall(classOf[ColorChange])
    println(s" [x] Event received => $message")
    if !(users.keySet contains message.UUID) then
      val otherBrush = Brush(0, 0, message.color)
      users.addOne(message.UUID -> otherBrush)
      brushManager.addBrush(otherBrush)
    else
      users(message.UUID).setColor(message.color)
    view.refresh()
  })

  private val mouseMoveQueue = registerDeliveryCallback(MOUSE_MOVE_EXCHANGE, (_, delivery) => {
    val message = delivery.unmarshall(classOf[MouseMove])
    if (users.keySet contains message.UUID) && (message.UUID != localUser._1) then
      println(s" [x] Event received => $message")
      users(message.UUID).updatePosition(message.x, message.y)
      view.refresh()
  })

  private val userExitQueue = registerDeliveryCallback(USER_EXIT_EXCHANGE, (_, delivery) => {
    val message = delivery.unmarshall(classOf[UserExit])
    val brush = users(message.UUID)
    users.remove(message.UUID)
    brushManager.removeBrush(brush)
    view.refresh()
  })

  private val recvQueues = Seq(pixelColorQueue, colorChangeQueue, mouseMoveQueue, userExitQueue)

  channel.basicConsume(STATE_REQUEST_QUEUE, true, (_, delivery) => {
    publishToQueue(StateReply(grid, users), delivery.getProperties().getReplyTo())
  }, _ => {})

  private val windowListener = new WindowAdapter() {
    override def windowClosing(e: WindowEvent): Unit =
      publishToExchange(UserExit(localUser._1), USER_EXIT_EXCHANGE)
      view.dispose()
      recvQueues.foreach(channel.queueDelete)
      channel.close()
      connection.close()
      System.exit(0)
  }

  view.addWindowListener(windowListener)
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
