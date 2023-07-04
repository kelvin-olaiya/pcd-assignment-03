package pcd.assignment03.ex1

import scala.collection.immutable.TreeSet

trait Leaderboard:
  def submit(path: String, lines: Int): Leaderboard
  def toList: List[(String, Int)]
  def merge(leaderboard: Leaderboard): Leaderboard

object Leaderboard:
  def apply(leaderboard: TreeSet[(String, Int)], numLongestFiles: Int): Leaderboard =
    LeaderboardImpl(leaderboard, numLongestFiles)

  def apply(numLongestFiles: Int): Leaderboard = Leaderboard(TreeSet(), numLongestFiles)

  private class LeaderboardImpl(
    private val leaderboard: TreeSet[(String, Int)],
    private val numLongestFiles: Int
  ) extends Leaderboard:

    def submit(path: String, lines: Int): Leaderboard =
      val tempLeaderboard = leaderboard + (path -> lines)
      if (leaderboard.size < numLongestFiles) {
        Leaderboard(tempLeaderboard, numLongestFiles)
      } else {
        Leaderboard(tempLeaderboard - leaderboard.minBy(_._2), numLongestFiles)
      }

    def toList: List[(String, Int)] = leaderboard.toList.sorted((a,b) => b._2 - a._2)

    def merge(leaderboard: Leaderboard): Leaderboard =
      Leaderboard((this.toList ++ leaderboard.toList).take(numLongestFiles).to(TreeSet), numLongestFiles)