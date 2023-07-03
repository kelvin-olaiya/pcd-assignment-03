import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, ActorSystem, Behavior, DispatcherSelector}
import akka.stream.scaladsl.FileIO

import java.nio.file.Paths
import scala.collection.immutable.TreeSet
import scala.concurrent.ExecutionContext

/*object FileAnalyzer:
  sealed trait Command
  case class Count(path: String, replyTo: ActorRef[Any]) extends Command
  def apply(path: String): Behavior[Command] = Behaviors.setup { context =>
    context.log.info(s"Analyzing $path file")
    implicit val executionContext: ExecutionContext =
      context.system.dispatchers.lookup(DispatcherSelector.fromConfig("my-blocking-dispatcher"))
//    val lines = FileIO.fromPath(Paths.get(path)).
  }

object DirectoryAnalyzer:
  sealed trait Command
  case class Analyze(path: String) extends Command
  case class Result(path: String, report: Report)

  def apply(path: String): Behavior[Command] = Behaviors.setup { context =>

  }
*/

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

/*object SourceAnalyzer:
  sealed trait Command
  case class Count(path: String) extends Command
  case class Result(path: String, report: Report) extends Command

  def apply(maxLines: Int = 1000, numIntervals: Int = 5, numLongestFiles: Int = 5): Behavior[Command] =

    analyzeBehavior(intervals, List[String]())

  private def analyzeBehavior(counter: Map[Range, Int], leaderboard: List[String], numLongestFiles: Int): Behavior[Command] =
    Behaviors.setup { context =>
      Behaviors.receiveMessage {
        case Count(path) =>
          context.log.info(s"Start analyzing $path")
          context.spawn(DirectoryAnalyzer(path), "directory-analyzer")
          Behaviors.same

        case Result(path, report) =>
          Behaviors.same
      }
    }
*/

object Main extends App:
  import Report.*
  // val system = ActorSystem(guardianBehavior = SourceAnalyzer(), name = "hello")

