package pcd.assignment03.ex3

import pcd.assignment03.ex2.pixelart.{Brush, BrushManager, PixelGrid, PixelGridView}
import pcd.assignment03.ex3.Server.{model, remoteModel}

import java.awt.event.{WindowAdapter, WindowEvent}
import java.rmi.{Remote, RemoteException}
import java.rmi.registry.{LocateRegistry, Registry}
import java.rmi.server.UnicastRemoteObject
import java.util.UUID
import scala.collection.concurrent.TrieMap
import scala.util.Random

object Server extends App:
  val model = ModelService()
  val remoteModel = UnicastRemoteObject.exportObject(model, 0).asInstanceOf[ModelService]
  ModelService.registry.rebind("modelService", remoteModel)
  println("Server created!")
