import scala.collection.immutable.TreeSet

class Leaderboard(
   private val leaderboard: TreeSet[(String, Int)],
   private val numLongestFiles: Int) {

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
}
