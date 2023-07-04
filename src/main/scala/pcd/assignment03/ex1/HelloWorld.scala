package pcd.assignment03.ex1

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, ActorSystem, Behavior, DispatcherSelector}
import akka.stream.scaladsl.{FileIO, Framing}
import pcd.assignment03.ex1.Report.*
import pcd.assignment03.ex1.Utils.SearchConfiguration

import java.io.File
import java.nio.file.{Files, Paths}
import scala.collection.immutable.TreeSet
import scala.concurrent.ExecutionContext

object FileAnalyzer:
  sealed trait Command
  case class Count(path: String, replyTo: ActorRef[DirectoryAnalyzer.Command]) extends Command

  def apply(searchConfiguration: SearchConfiguration): Behavior[Command] = Behaviors.setup { context =>
    Behaviors.receiveMessage {
      case Count(path, replyTo) =>
        val report = Report(Files.lines(File(path).toPath).count().toInt, searchConfiguration)
        replyTo ! DirectoryAnalyzer.Result(path, report)
        Behaviors.same
    }
  }

object DirectoryAnalyzer:
  sealed trait Command
  case class Result(path: String, report: Report) extends Command

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
    directoryAnalyzer(requests.filter(_.isFile).map(_.getAbsolutePath), Report.empty(searchConfiguration), replyTo)
  }

  def directoryAnalyzer(
    pendingRequests: Set[String],
    report: Report,
    replyTo: ActorRef[SourceAnalyzer.Command]
  ): Behavior[Command] = Behaviors.receiveMessage {
    case Result(p, r) =>
      val pending = pendingRequests - p
      if (pending.isEmpty) {
        replyTo ! SourceAnalyzer.Result(p, report.merge(r), ???)
        Behaviors.stopped
      } else {
        directoryAnalyzer(pendingRequests - p, report.merge(r), replyTo)
      }
  }


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
          val searchConfiguration = SearchConfiguration(maxLines, numIntervals, numLongestFiles)
          context.spawn(DirectoryAnalyzer(path, searchConfiguration, context.self), "directory-analyzer")
          analyzeBehavior(Report(maxLines, numIntervals), Leaderboard(numLongestFiles), searchConfiguration, path)
      }
    }

  private def analyzeBehavior(
     report: Report,
     leaderboard: Leaderboard,
     searchConfig: SearchConfiguration,
     rootPath: String
  ): Behavior[Command] = Behaviors.receiveMessage {
    case Result(p, r, l) =>
      if (p == rootPath) {
        // TODO: Statistics
        Behaviors.stopped
      } else {
        analyzeBehavior(report merge r, leaderboard merge l, searchConfig, rootPath)
      }
    }

object Main extends App:
  // val system = ActorSystem(guardianBehavior = SourceAnalyzer(), name = "hello")

