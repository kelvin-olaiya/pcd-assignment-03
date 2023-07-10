package pcd.assignment03.ex3

import pcd.assignment03.ex2.pixelart.{Brush, PixelGrid}

import java.rmi.{Remote, RemoteException}
import java.rmi.registry.{LocateRegistry, Registry}
import scala.collection.concurrent.TrieMap

trait ModelService extends Remote:
  @throws(classOf[RemoteException]) def join(uuid: String, brush: Brush): java.util.Map[String, (RemoteObserver, Brush)]
  @throws(classOf[RemoteException]) def getGrid: PixelGrid
  @throws(classOf[RemoteException]) def changeUserColor(uuid: String, color: Int): Unit
  @throws(classOf[RemoteException]) def colorPixel(uuid: String, x: Int, y: Int): Unit
  @throws(classOf[RemoteException]) def leave(uuid: String): Unit

object ModelService:
  private val localRegistry = LocateRegistry.getRegistry(null)
  @throws(classOf[RemoteException]) def apply(): ModelService = ModelServiceImpl()
  @throws(classOf[RemoteException]) def registry: Registry = localRegistry

  private case class ModelServiceImpl() extends ModelService:
    val users: TrieMap[String, (RemoteObserver, Brush)] = TrieMap[String, (RemoteObserver, Brush)]()
    val grid: PixelGrid = PixelGrid(40, 40)

    def join(uuid: String, brush: Brush): java.util.Map[String, (RemoteObserver, Brush)] = synchronized {
      val remote = registry.lookup(uuid).asInstanceOf[RemoteObserver]
      users.foreach((_, p) => p._1.onUserJoin(uuid, remote, brush))
      val copy = Map.from(users)
      users.put(uuid, (remote, brush))
      scala.jdk.javaapi.CollectionConverters.asJava(copy)
    }

    def getGrid: PixelGrid = grid

    def changeUserColor(uuid: String, color: Int): Unit = synchronized {
      users(uuid)._2.setColor(color)
    }
    def colorPixel(uuid: String, x: Int, y: Int): Unit = synchronized {
      val color = users(uuid)._2.getColor
      grid.set(x, y, color)
      users.foreach((_, p) => p._1.onPixelColor(x, y, color))
    }

    def leave(uuid: String): Unit = synchronized {
      users.remove(uuid)
    }

