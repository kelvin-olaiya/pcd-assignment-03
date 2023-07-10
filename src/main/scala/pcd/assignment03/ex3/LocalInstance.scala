package pcd.assignment03.ex3

import pcd.assignment03.ex2.pixelart.{Brush, BrushManager, PixelGrid, PixelGridView}

import scala.collection.concurrent.TrieMap

class LocalInstance() extends RemoteObserver:
  val user: User = User()
  val brushManager: BrushManager = BrushManager()
  var others: TrieMap[String, (RemoteObserver, Brush)] = TrieMap[String, (RemoteObserver, Brush)]()
  val grid: PixelGrid = PixelGrid(40, 40)
  brushManager.addBrush(user.brush)
  val view = new PixelGridView(grid, brushManager, 800, 800)

  def onUserJoin(uuid: String, remote: RemoteObserver, brush: Brush): Unit = synchronized {
    others.addOne(uuid, (remote, brush))
    brushManager.addBrush(brush)
    println("user added")
  }

  def onMouseMoved(remote: String, x: Int, y: Int): Unit = synchronized {
    others(remote)._2.updatePosition(x, y)
    view.refresh()
    brushManager.addBrush(user.brush)
    println("remote position update")
  }

  def onUserColorChange(uuid: String, color: Int): Unit = synchronized {
    others(uuid)._2.setColor(color)
    view.refresh()
    println("remote color change")
  }

  def onPixelColor(x: Int, y: Int, color: Int): Unit = synchronized {
    grid.set(x, y, color)
    view.refresh()
  }

  def onUserExit(uuid: String): Unit = synchronized {
    val removedBrush = others.remove(uuid)
    removedBrush.foreach((_, b) => brushManager.removeBrush(b))
    view.refresh()
    println("remote brush removed")
  }
