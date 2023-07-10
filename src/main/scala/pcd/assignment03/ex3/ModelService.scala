package pcd.assignment03.ex3

import pcd.assignment03.ex2.pixelart.{Brush, PixelGrid}

import java.rmi.{Remote, RemoteException}
import java.rmi.registry.{LocateRegistry, Registry}
import scala.collection.concurrent.TrieMap

trait ModelService extends Remote:
  @throws(classOf[RemoteException]) def join(uuid: String, brush: Brush): java.util.Map[RemoteObserver, Brush]
  @throws(classOf[RemoteException]) def getGrid: PixelGrid
  @throws(classOf[RemoteException]) def changeUserColor(remote: RemoteObserver, color: Int): Unit
  @throws(classOf[RemoteException]) def colorPixel(remote: RemoteObserver, x: Int, y: Int): Unit
  @throws(classOf[RemoteException]) def leave(remote: RemoteObserver): Unit

object ModelService:
  private val localRegistry = LocateRegistry.getRegistry(null)
  @throws(classOf[RemoteException]) def apply(): ModelService = ModelServiceImpl()
  @throws(classOf[RemoteException]) def registry: Registry = localRegistry

  private case class ModelServiceImpl() extends ModelService:
    val users: TrieMap[RemoteObserver, Brush] = TrieMap[RemoteObserver, Brush]()
    val grid: PixelGrid = PixelGrid(40, 40)

    def join(uuid: String, brush: Brush): java.util.Map[RemoteObserver, Brush] = synchronized {
      val remote = registry.lookup(uuid).asInstanceOf[RemoteObserver]
      users.foreach((u, _) => u.onUserJoin(remote, brush))
      val copy = Map.from(users)
      users.put(remote, brush)
      scala.jdk.javaapi.CollectionConverters.asJava(copy)
    }

    def getGrid: PixelGrid = grid

    def changeUserColor(remote: RemoteObserver, color: Int): Unit = synchronized {
      users(remote).setColor(color)
    }
    def colorPixel(remote: RemoteObserver, x: Int, y: Int): Unit = synchronized {
      val color = users(remote).getColor
      grid.set(x, y, color)
      users.foreach((u, _) => u.onPixelColor(x, y, color))
    }

    def leave(remote: RemoteObserver): Unit = synchronized {
      users.remove(remote)
    }

