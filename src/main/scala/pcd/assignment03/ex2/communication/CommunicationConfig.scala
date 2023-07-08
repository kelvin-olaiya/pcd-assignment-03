package pcd.assignment03.ex2.communication

import com.rabbitmq.client
import com.rabbitmq.client.{Channel, ConnectionFactory}

object CommunicationConfig:
  val connection: client.Connection =
    val factory = ConnectionFactory()
    factory.setHost("localhost") // 0.tcp.eu.ngrok.io
    // factory.setPort(16792)
    // factory.setUsername("guest")
    // factory.setPassword("guest")
    factory.newConnection()

  val channel: Channel =
    connection.createChannel()

  val COLOR_CHANGE_EXCHANGE: String = "color_change"
  val PIXEL_COLOR_EXCHANGE: String = "pixel_color"
  val MOUSE_MOVE_EXCHANGE: String = "mouse_move"
  val USER_EXIT_EXCHANGE: String = "user_exit"

  val STATE_REQUEST_QUEUE: String = "state_request"
