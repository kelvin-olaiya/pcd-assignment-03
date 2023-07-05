package pcd.assignment03.ex1

trait View:
  def terminated(): Unit

  def updateReport(report: Report): Unit

  def updateLeaderboard(leaderboard: Leaderboard): Unit

