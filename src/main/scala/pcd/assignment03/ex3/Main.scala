package pcd.assignment03.ex3

import pcd.assignment03.ex2.pixelart.{Brush, BrushManager, PixelGrid, PixelGridView}
import pcd.assignment03.ex3.Main.{model, remoteModel}
import pcd.assignment03.ex3.User.user

import java.awt.event.{WindowAdapter, WindowEvent}
import java.rmi.{Remote, RemoteException}
import java.rmi.registry.{LocateRegistry, Registry}
import java.rmi.server.UnicastRemoteObject
import java.util.UUID
import scala.collection.concurrent.TrieMap
import scala.util.Random


trait ModelService extends Remote:
  @throws(classOf[RemoteException]) def join(uuid: String, brush: Brush): java.util.Map[RemoteUser, Brush]
  @throws(classOf[RemoteException]) def getGrid: PixelGrid
  @throws(classOf[RemoteException]) def userChangeColor(remote: RemoteUser, color: Int): Unit
  @throws(classOf[RemoteException]) def color(remote: RemoteUser, x: Int, y: Int): Unit
  @throws(classOf[RemoteException]) def leave(remote: RemoteUser): Unit

case class ModelServiceImpl() extends ModelService:
  val users: TrieMap[RemoteUser, Brush] = TrieMap[RemoteUser, Brush]()
  val grid: PixelGrid = PixelGrid(40, 40)
  val registry: Registry = LocateRegistry.getRegistry(null)

  def join(uuid: String, brush: Brush): java.util.Map[RemoteUser, Brush] = synchronized {
    val remote = registry.lookup(uuid).asInstanceOf[RemoteUser]
    users.foreach((u, _) => u.newUserJoin(remote, brush))
    val copy = Map.from(users)
    users.put(remote, brush)
    scala.jdk.javaapi.CollectionConverters.asJava(copy)
  }

  def getGrid: PixelGrid = grid

  def userChangeColor(remote: RemoteUser, color: Int): Unit = synchronized {
    users(remote).setColor(color)
  }

  def color(remote: RemoteUser, x: Int, y: Int): Unit = synchronized {
    val color = users(remote).getColor
    grid.set(x, y, color)
    users.foreach((u, _) => u.color(x, y, color))
  }

  def leave(remote: RemoteUser): Unit = synchronized {
    users.remove(remote)
  }

object Main extends App:
  val model = ModelServiceImpl()
  val remoteModel = UnicastRemoteObject.exportObject(model, 0).asInstanceOf[ModelService]
  model.registry.rebind("modelService", remoteModel)
  println("Server created!")

trait RemoteUser extends Remote:
  @throws(classOf[RemoteException]) def newUserJoin(remote: RemoteUser, brush: Brush): Unit
  @throws(classOf[RemoteException]) def userMovedMouse(remote: RemoteUser, x: Int, y: Int): Unit
  @throws(classOf[RemoteException]) def userChangeColor(remote: RemoteUser, color: Int): Unit
  @throws(classOf[RemoteException]) def color(x: Int, y: Int, color: Int): Unit
  @throws(classOf[RemoteException]) def userLeave(remote: RemoteUser): Unit

trait LocalUser:
  def userId: UUID
  def brush: Brush

case class UserImpl() extends RemoteUser with LocalUser:
  val userId: UUID = UUID.randomUUID()
  val brush: Brush = Brush(0, 0, Random.nextInt(256 * 256 * 256))
  val brushManager: BrushManager = BrushManager()
  var remoteUsers: TrieMap[RemoteUser, Brush] = TrieMap[RemoteUser, Brush]()

  val grid: PixelGrid = PixelGrid(40, 40)
  brushManager.addBrush(brush)
  val view = new PixelGridView(grid, brushManager, 800, 800)

  def newUserJoin(remote: RemoteUser, brush: Brush): Unit = synchronized {
    remoteUsers.addOne(remote, brush)
    brushManager.addBrush(brush)
    println("user added")
  }

  def userMovedMouse(remote: RemoteUser, x: Int, y: Int): Unit = synchronized {
    remoteUsers(remote).updatePosition(x, y)
    view.refresh()
    brushManager.addBrush(brush)
    println("remote position update")
  }

  def userChangeColor(remote: RemoteUser, color: Int): Unit = synchronized {
    remoteUsers(remote).setColor(color)
    view.refresh()
    println("remote color change")
  }

  def color(x: Int, y: Int, color: Int): Unit = synchronized{
    grid.set(x, y, color)
    view.refresh()
  }

  def userLeave(remote: RemoteUser): Unit = synchronized {
    val removedBrush = remoteUsers.remove(remote)
    removedBrush.foreach(brushManager.removeBrush)
    view.refresh()
    println("remote brush removed")
  }

object User extends App:
  val registry: Registry = LocateRegistry.getRegistry(null)
  val user = UserImpl()
  val stub = UnicastRemoteObject.exportObject(user, 0).asInstanceOf[RemoteUser]
  registry.rebind(user.userId.toString, user)
  val model = registry.lookup("modelService").asInstanceOf[ModelService]

  user.remoteUsers = scala.jdk.javaapi.CollectionConverters.asScala(model.join(user.userId.toString, user.brush)).to(TrieMap)
  val status = model.getGrid
  for
    i <- 0 until 40
    j <- 0 until 40
  do user.grid.set(i, j, status.get(i, j))
  user.remoteUsers.values.foreach(user.brushManager.addBrush)

  user.view.addMouseMovedListener((x: Int, y: Int) => {
    user.remoteUsers.foreach((u, _) => u.userMovedMouse(user, x, y))
    user.brush.updatePosition(x, y)
    user.view.refresh()
  })

  user.view.addColorChangedListener(color => {
    user.brush.setColor(color)
    user.remoteUsers.foreach((u, _) => u.userChangeColor(user, color))
    model.userChangeColor(user, color)
    user.view.refresh()
  })

  user.view.addPixelGridEventListener((x, y) => {
    model.color(user, x, y)
  })

  private val windowListener = new WindowAdapter() {
    override def windowClosing(e: WindowEvent): Unit =
      model.leave(user)
      user.remoteUsers.foreach((u, _) => u.userLeave(user))
      user.view.dispose()
      System.exit(0)
  }

  user.view.addWindowListener(windowListener)

  user.view.display()