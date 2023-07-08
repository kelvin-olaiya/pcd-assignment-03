package pcd.assignment03.ex2.communication

import com.rabbitmq.client.AMQP.Queue
import com.rabbitmq.client.{AMQP, DeliverCallback}
import pcd.assignment03.ex2.{Message, Utils}
import pcd.assignment03.ex2.Utils.gson

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

