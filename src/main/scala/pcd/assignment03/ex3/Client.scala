package pcd.assignment03.ex3

import java.awt.event.{WindowAdapter, WindowEvent}
import java.rmi.registry.{LocateRegistry, Registry}
import java.rmi.server.UnicastRemoteObject
import scala.collection.concurrent.TrieMap

object Client extends App:
  val localInstance = LocalInstance()
  val registry: Registry = LocateRegistry.getRegistry(null)
  val stub = UnicastRemoteObject.exportObject(localInstance, 0).asInstanceOf[RemoteObserver]
  registry.rebind(localInstance.user.userId.toString, stub)
  val model = registry.lookup("modelService").asInstanceOf[ModelService]

  localInstance.others =
    scala.jdk.javaapi.CollectionConverters.asScala(
      model.join(localInstance.user.userId.toString, localInstance.user.brush)
    ).to(TrieMap)

  val status = model.getGrid
  for
    i <- 0 until 40
    j <- 0 until 40
  do localInstance.grid.set(i, j, status.get(i, j))
  localInstance.others.values.foreach(localInstance.brushManager.addBrush)

  localInstance.view.addMouseMovedListener((x: Int, y: Int) => {
    localInstance.others.foreach((u, _) => u.onMouseMoved(localInstance, x, y))
    localInstance.user.brush.updatePosition(x, y)
    localInstance.view.refresh()
  })

  localInstance.view.addColorChangedListener(color => {
    localInstance.user.brush.setColor(color)
    localInstance.others.foreach((u, _) => u.onUserColorChange(localInstance, color))
    model.changeUserColor(localInstance, color)
    localInstance.view.refresh()
  })

  localInstance.view.addPixelGridEventListener((x, y) => {
    model.colorPixel(localInstance, x, y)
  })

  private val windowListener = new WindowAdapter() {
    override def windowClosing(e: WindowEvent): Unit =
      model.leave(localInstance)
      localInstance.others.foreach((u, _) => u.onUserExit(localInstance))
      registry.unbind(localInstance.user.userId.toString)
      localInstance.view.dispose()
      System.exit(0)
  }

  localInstance.view.addWindowListener(windowListener)
  localInstance.view.display()