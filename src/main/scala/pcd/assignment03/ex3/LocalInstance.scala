package pcd.assignment03.ex3

import pcd.assignment03.ex2.pixelart.{Brush, BrushManager, PixelGrid, PixelGridView}

import scala.collection.concurrent.TrieMap

class LocalInstance() extends RemoteObserver:
  val user: User = User()
  val brushManager: BrushManager = BrushManager()
  var others: TrieMap[RemoteObserver, Brush] = TrieMap[RemoteObserver, Brush]()
  val grid: PixelGrid = PixelGrid(40, 40)
  brushManager.addBrush(user.brush)
  val view = new PixelGridView(grid, brushManager, 800, 800)

  def onUserJoin(remote: RemoteObserver, brush: Brush): Unit = synchronized {
    others.addOne(remote, brush)
    brushManager.addBrush(brush)
    println("user added")
  }

  def onMouseMoved(remote: RemoteObserver, x: Int, y: Int): Unit = synchronized {
    others(remote).updatePosition(x, y)
    view.refresh()
    brushManager.addBrush(user.brush)
    println("remote position update")
  }

  def onUserColorChange(remote: RemoteObserver, color: Int): Unit = synchronized {
    others(remote).setColor(color)
    view.refresh()
    println("remote color change")
  }

  def onPixelColor(x: Int, y: Int, color: Int): Unit = synchronized {
    grid.set(x, y, color)
    view.refresh()
  }

  def onUserExit(remote: RemoteObserver): Unit = synchronized {
    val removedBrush = others.remove(remote)
    removedBrush.foreach(brushManager.removeBrush)
    view.refresh()
    println("remote brush removed")
  }
