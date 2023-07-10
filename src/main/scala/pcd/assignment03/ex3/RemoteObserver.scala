package pcd.assignment03.ex3

import pcd.assignment03.ex2.pixelart.Brush

import java.rmi.{Remote, RemoteException}

trait RemoteObserver extends Remote:
  @throws(classOf[RemoteException]) def onUserJoin(uuid: String, remote: RemoteObserver, brush: Brush): Unit
  @throws(classOf[RemoteException]) def onMouseMoved(uuid: String, x: Int, y: Int): Unit
  @throws(classOf[RemoteException]) def onUserColorChange(uuid: String, color: Int): Unit
  @throws(classOf[RemoteException]) def onPixelColor(x: Int, y: Int, color: Int): Unit
  @throws(classOf[RemoteException]) def onUserExit(uuid: String): Unit
