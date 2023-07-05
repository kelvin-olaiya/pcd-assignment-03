package pcd.assignment03.ex1.static.view

import akka.actor.typed.ActorSystem
import pcd.assignment03.ex1.dynamic.Utils.SearchConfiguration
import pcd.assignment03.ex1.dynamic.model.{Leaderboard, Report}
import pcd.assignment03.ex1.dynamic.{Manager, View}
import pcd.assignment03.ex1.{Leaderboard, Manager, Report, View}

class CLI(system: ActorSystem[Manager.Command]) extends View:

  println("Inserisci percorso: ")
  val path = scala.io.StdIn.readLine()

  println("Estremo massimo: ")
  val max = scala.io.StdIn.readInt()

  println("Numero intervalli: ")
  val numIntervals = scala.io.StdIn.readInt()

  println("Numero classificati: ")
  val numFinalists = scala.io.StdIn.readInt()



  val searchConfiguration: SearchConfiguration = SearchConfiguration(max, numIntervals, numFinalists)

  println("----------------")

  private var report = Report(searchConfiguration.maxLines, searchConfiguration.numIntervals)
  private var leaderboard = Leaderboard(searchConfiguration.numLongestFile)

  system ! Manager.Start(path, searchConfiguration, this)



  override def terminated(): Unit =
    println(report)
    println("---------------")
    println(leaderboard)

  override def updateReport(r: Report): Unit = report = r

  override def updateLeaderboard(l: Leaderboard): Unit = leaderboard = l


