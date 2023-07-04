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

object SourceAnalyzer:
  import Leaderboard.*
  sealed trait Command
  case class Count(path: String) extends Command
  case class Result(path: String, report: Report, leaderboard: Leaderboard) extends Command

  def apply(maxLines: Int = 1000, numIntervals: Int = 5, numLongestFiles: Int = 5): Behavior[Command] =
    Behaviors.setup { context => 
      Behaviors.receiveMessage {
        case Count(path) => 
          context.log.info(s"Spawning directory analyzer for path $path")
          context.spawn(DirectoryAnalyzer(path), "directory-analyzer")
          analyzeBehavior(Report(maxLines, numIntervals), Leaderboard(numLongestFiles), path)
      }
    }

  private def analyzeBehavior(report: Report, leaderboard: Leaderboard, rootPath: String): Behavior[Command] =
    Behaviors.setup { context =>
      Behaviors.receiveMessage {
        case Result(p, r, l) =>
          if (p == rootPath) {
            Behaviors.stopped
          } else {
            analyzeBehavior(report merge r, leaderboard merge l, rootPath)
          }
      }
    }

object Main extends App:
  import Report.*
  // val system = ActorSystem(guardianBehavior = SourceAnalyzer(), name = "hello")

