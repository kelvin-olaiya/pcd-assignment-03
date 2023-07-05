package pcd.assignment03.ex1.dynamic.view

import pcd.assignment03.ex1.dynamic.model.{Leaderboard, Report}

trait View:
  def terminated(): Unit

  def updateReport(report: Report): Unit

  def updateLeaderboard(leaderboard: Leaderboard): Unit

