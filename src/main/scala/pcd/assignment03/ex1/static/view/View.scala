package pcd.assignment03.ex1.static.view

import pcd.assignment03.ex1.dynamic.model.{Leaderboard, Report}
import pcd.assignment03.ex1.{Leaderboard, Report}

trait View:
  def terminated(): Unit

  def updateReport(report: Report): Unit

  def updateLeaderboard(leaderboard: Leaderboard): Unit

