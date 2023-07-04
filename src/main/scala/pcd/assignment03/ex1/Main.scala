package pcd.assignment03.ex1

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, ActorSystem, Behavior, DispatcherSelector}
import pcd.assignment03.ex1.Report.*
import pcd.assignment03.ex1.Utils.SearchConfiguration

import java.beans.Beans
import java.io.File
import java.nio.file.{Files, Paths}
import scala.collection.immutable.TreeSet
import scala.concurrent.ExecutionContext

object FileAnalyzer:
  sealed trait Command
  case class Count(path: String, replyTo: ActorRef[DirectoryAnalyzer.Command]) extends Command

  def apply(searchConfiguration: SearchConfiguration): Behavior[Command] = Behaviors.receiveMessage {
    case Count(path, replyTo) =>
      val lines = Files.lines(File(path).toPath).count().toInt
      val report = Report(lines, searchConfiguration)
      val leaderboard = Leaderboard(searchConfiguration.numLongestFile).submit(path, lines)
      replyTo ! DirectoryAnalyzer.Result(path, report, leaderboard)
      Behaviors.same
  }

object DirectoryAnalyzer:
  sealed trait Command
  case class Result(path: String, report: Report, leaderboard: Leaderboard) extends Command

  def apply(
    path: String,
    searchConfiguration: SearchConfiguration,
    replyTo: ActorRef[SourceAnalyzer.Command]
  ): Behavior[Command] = Behaviors.setup { context =>
    val fileAnalyzer = context.spawn(FileAnalyzer(searchConfiguration), "file-analyzer", DispatcherSelector.fromConfig("file-dispatcher"))
    val requests = File(path).listFiles(f => f.isDirectory || f.isFile && f.getName.endsWith("java")).toSet
    requests.foreach { f =>
      if (f.isDirectory) {
        context.spawn(DirectoryAnalyzer(f.getAbsolutePath, searchConfiguration, replyTo), "directory-analyzer")
      } else if (f.isFile && f.getName.endsWith("java")) {
        fileAnalyzer ! FileAnalyzer.Count(f.getAbsolutePath, context.self)
      }
    }
    directoryAnalyzer(requests.filter(_.isFile).map(_.getAbsolutePath),
      Report.empty(searchConfiguration),
      Leaderboard(searchConfiguration.numLongestFile),
      replyTo)
  }

  def directoryAnalyzer(
    pendingRequests: Set[String],
    report: Report,
    leaderboard: Leaderboard,
    replyTo: ActorRef[SourceAnalyzer.Command]
  ): Behavior[Command] = Behaviors.receiveMessage {
    case Result(p, r, l) =>
      val pending = pendingRequests - p
      if (pending.isEmpty) {
        replyTo ! SourceAnalyzer.Result(p, report merge r, leaderboard merge l)
        Behaviors.stopped
      } else {
        directoryAnalyzer(pendingRequests - p, report.merge(r), leaderboard, replyTo)
      }
  }

object LeaderboardActor:
  sealed trait Command
  case class Update(leaderboard: Leaderboard) extends Command
  case class Request(replyTo: ActorRef[SourceAnalyzer.Command]) extends Command

  def apply(numLongestFiles: Int, notifyTo: ActorRef[GUIActor.Command]): Behavior[Command] =
    leaderboardActor(Leaderboard(numLongestFiles))

  private def leaderboardActor(leaderboard: Leaderboard): Behavior[Command] =
    Behaviors.receiveMessage {
      case Update(l) =>
        // TODO: notifyGUI
        leaderboardActor(leaderboard merge l)
      case Request(replyTo) =>
        replyTo ! SourceAnalyzer.Response(leaderboard)
        Behaviors.same
    }

object SourceAnalyzer:
  import Leaderboard.*
  sealed trait Command
  case class Count(path: String) extends Command
  case class Result(path: String, report: Report, leaderboard: Leaderboard) extends Command
  case class Response(leaderboard: Leaderboard) extends Command
  case class Halt() extends Command

  def apply(maxLines: Int = 1000, numIntervals: Int = 5, numLongestFiles: Int = 5): Behavior[Command] =
    Behaviors.setup { context =>
      Behaviors.receiveMessage {
        case Count(path) =>
          context.log.info(s"Spawning directory analyzer for path $path")
          val searchConfiguration = SearchConfiguration(maxLines, numIntervals, numLongestFiles)
          val guiActor = context.spawn(GUIActor(), "gui-actor")
          context.spawn(DirectoryAnalyzer(path, searchConfiguration, context.self), "directory-analyzer")
          context.spawn(LeaderboardActor(numLongestFiles, guiActor), "leaderboard-actor")
          analyzeBehavior(Report(maxLines, numIntervals), searchConfiguration, path)
      }
    }

  private def analyzeBehavior(
     report: Report,
     searchConfig: SearchConfiguration,
     rootPath: String
  ): Behavior[Command] = Behaviors.receiveMessage {
    case Result(p, r, l) =>
      if (p == rootPath) {
        // TODO: Statistics
        Behaviors.stopped
      } else {
        analyzeBehavior(report merge r, searchConfig, rootPath)
      }
    case Halt() =>
      Behaviors.stopped
    }

object GUIActor:
  sealed trait Command
  case class UpdateLeaderboard(leaderboard: Leaderboard) extends Command
  case class UpdateReport(report: Report) extends Command

  def apply(): Behavior[Command] = GUIActor()
  private def GUIActor(): Behavior[Command] =
    Behaviors.receiveMessage {
      case UpdateLeaderboard(l) => ???
      case UpdateReport(r) => ???
    }

object Main extends App:
  // val system = ActorSystem(guardianBehavior = SourceAnalyzer(), name = "hello")
  println("ciao")