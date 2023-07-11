package pcd.assignment03.ex3

import java.awt.event.{WindowAdapter, WindowEvent}
import java.rmi.registry.{LocateRegistry, Registry}
import java.rmi.server.UnicastRemoteObject
import scala.collection.concurrent.TrieMap
import scala.io.StdIn

object Client extends App:
  val registry: Registry = LocateRegistry.getRegistry(null)

  print("Other user UUID: ")
  val remoteUUID = StdIn.readLine()
  val model: ModelService =
    if remoteUUID.nonEmpty then
      registry.lookup(remoteUUID).asInstanceOf[RemoteObserver].getModelService
    else
      print("Session name: ")
      val sessionName = StdIn.readLine()
      if sessionName.nonEmpty then
        registry.lookup(sessionName).asInstanceOf[ModelService]
      else registry.lookup("modelService").asInstanceOf[ModelService]

  private val localInstance = LocalInstance(model)
  private val stub = UnicastRemoteObject.exportObject(localInstance, 0).asInstanceOf[RemoteObserver]
  registry.rebind(localInstance.user.userId.toString, stub)

  println(s"My UUID: ${localInstance.user.userId}")

  localInstance.others =
    scala.jdk.javaapi.CollectionConverters.asScala(
      model.join(localInstance.user.userId.toString, localInstance.user.brush)
    ).to(TrieMap)

  private val status = model.getGrid
  for
    i <- 0 until 40
    j <- 0 until 40
  do localInstance.grid.set(i, j, status.get(i, j))
  localInstance.others.values.foreach(p => localInstance.brushManager.addBrush(p._2))

  localInstance.view.addMouseMovedListener((x: Int, y: Int) => {
    localInstance.others.foreach((_, p) => p._1.onMouseMoved(localInstance.user.userId.toString, x, y))
    localInstance.user.brush.updatePosition(x, y)
    localInstance.view.refresh()
  })

  localInstance.view.addColorChangedListener(color => {
    localInstance.user.brush.setColor(color)
    localInstance.others.foreach((_, p) => p._1.onUserColorChange(localInstance.user.userId.toString, color))
    model.changeUserColor(localInstance.user.userId.toString, color)
    localInstance.view.refresh()
  })

  localInstance.view.addPixelGridEventListener(model.colorPixel(localInstance.user.userId.toString, _, _))

  private val windowListener = new WindowAdapter() {
    override def windowClosing(e: WindowEvent): Unit =
      model.leave(localInstance.user.userId.toString)
      localInstance.others.foreach((_, p) => p._1.onUserExit(localInstance.user.userId.toString))
      registry.unbind(localInstance.user.userId.toString)
      localInstance.view.dispose()
      System.exit(0)
  }

  localInstance.view.addWindowListener(windowListener)
  localInstance.view.display()
  