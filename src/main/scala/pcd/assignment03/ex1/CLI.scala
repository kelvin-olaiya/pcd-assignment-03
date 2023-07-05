package pcd.assignment03.ex1

import akka.actor.typed.ActorSystem
import pcd.assignment03.ex1.Utils.SearchConfiguration

class CLI(searchConfiguration: SearchConfiguration) extends View:
  private var report = Report(searchConfiguration.maxLines, searchConfiguration.maxLines)
  private var leaderboard = Leaderboard(searchConfiguration.numLongestFile)

  override def terminated(): Unit =
    println(report)
    println("---------------")
    println(leaderboard)

  override def updateReport(r: Report): Unit = report = report.merge(r)

  override def updateLeaderboard(l: Leaderboard): Unit = leaderboard = leaderboard.merge(l)


