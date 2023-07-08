package pcd.assignment03.ex2

import com.google.gson.reflect.TypeToken
import com.google.gson.stream.{JsonReader, JsonWriter}
import com.google.gson.*
import com.rabbitmq.client
import com.rabbitmq.client.AMQP.{BasicProperties, Queue}
import com.rabbitmq.client.impl.AMQImpl.Connection
import com.rabbitmq.client.*
import pcd.assignment03.ex2.MapTypeAdapter
import pcd.assignment03.ex2.pixelart.{Brush, BrushManager, PixelGrid, PixelGridView}

import java.awt.event.{WindowAdapter, WindowEvent}
import java.awt.{Color, Label, PopupMenu}
import java.lang.reflect
import java.util.UUID
import java.util.concurrent.{ArrayBlockingQueue, BlockingQueue, CompletableFuture, LinkedBlockingQueue, TimeUnit, TimeoutException}
import javax.swing.{JButton, JFrame, JPanel, WindowConstants}
import scala.collection.concurrent.TrieMap
import scala.quoted.Type
import scala.reflect.ClassTag
import scala.util.Random

object Utils:

  import Message.*
  import communication.CommunicationApi.*
  import communication.CommunicationConfig.*

  val gson: Gson = GsonBuilder()
    .registerTypeAdapter(classOf[TrieMap[UUID, Brush]], MapTypeAdapter)
    .create()

  extension (delivery: Delivery) {
    private def asString = String(delivery.getBody, "UTF-8")
    def unmarshall[T](cls: Class[T]): T = gson.fromJson(delivery.asString, cls)
  }

  val eventBuffer: BlockingQueue[PixelColor] = LinkedBlockingQueue[PixelColor]()

enum Message extends Serializable:
  case PixelColor(x: Int, y: Int, UUID: UUID)
  case ColorChange(color: Int, UUID: UUID)
  case MouseMove(x: Int, y: Int, UUID: UUID)
  case UserExit(UUID: UUID)
  case StateRequest()
  case StateReply(grid: PixelGrid, users: TrieMap[UUID, Brush])

object Main extends App:

  import Message.*
  import Utils.*
  import communication.CommunicationApi.*
  import communication.CommunicationConfig.*

  private def requestGrid(): PixelGrid =
    var grid = PixelGrid(40, 40)
    val responseQueue: String = channel.queueDeclare.getQueue
    val props = BasicProperties().builder()
      .replyTo(responseQueue)
      .build()
    publishToQueue(StateRequest(), STATE_REQUEST_QUEUE, props)
    val future = CompletableFuture[StateReply]()
    val cancellationTag = channel.basicConsume(responseQueue, true, (_, delivery) => {
      val message = delivery.unmarshall(classOf[StateReply])
      future.complete(message)
    }, _ => {})

    try {
      val result = future.get(5, TimeUnit.SECONDS)
      Thread.sleep(40_000)
      grid = result.grid
      users.addAll(result.users)
      eventBuffer.forEach(message => grid.set(message.x, message.y, users(message.UUID).getColor))
      eventBuffer.clear()
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
  private val localUser = (UUID.randomUUID(), Brush(0, 0, Random.nextInt(256 * 256 * 256)))
  var grid: Option[PixelGrid] = None

  private val pixelColorQueue = registerDeliveryCallback(PIXEL_COLOR_EXCHANGE, (_, delivery) => {
    val message = delivery.unmarshall(classOf[PixelColor])
    grid.foreach(_.set(message.x, message.y, users(message.UUID).getColor))
    if grid.isEmpty then {
      eventBuffer.put(message)
    }
    view.refresh()
  })

  private val colorChangeQueue = registerDeliveryCallback(COLOR_CHANGE_EXCHANGE, (_, delivery) => {
    val message = delivery.unmarshall(classOf[ColorChange])
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

  grid = Some(requestGrid())
  users.addOne(localUser._1 -> localUser._2)
  users.foreach((_, b) => brushManager.addBrush(b))
  val view = new PixelGridView(grid.get, brushManager, 800, 800)

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

  channel.basicConsume(STATE_REQUEST_QUEUE, true, (_, delivery) => {
    grid.foreach(grid => publishToQueue(StateReply(grid, users), delivery.getProperties().getReplyTo()))
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
