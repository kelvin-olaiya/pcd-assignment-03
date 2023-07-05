package pcd.assignment03.ex1

import akka.actor.typed.{ActorRef, Behavior, DispatcherSelector}
import akka.actor.typed.scaladsl.Behaviors
import pcd.assignment03.ex1.Utils.SearchConfiguration
import Utils.freshLabel

import java.io.File

object DirectoryAnalyzer:
  sealed trait Command
  case class Result(path: String, report: Report, leaderboard: Leaderboard) extends Command
  case class Ack(path: String) extends Command

  def apply(
    path: String,
    searchConfiguration: SearchConfiguration,
    sourceAnalyzer: ActorRef[SourceAnalyzer.Command],
    parent: ActorRef[DirectoryAnalyzer.Ack],
    leaderboardActor: ActorRef[LeaderboardActor.Command]
  ): Behavior[Command] = Behaviors.setup { context =>
    val fileAnalyzer = context.spawn(FileAnalyzer(searchConfiguration), path.freshLabel("file-analyzer"), DispatcherSelector.fromConfig("file-dispatcher"))
    val requests = File(path).listFiles(f => f.isDirectory || f.isFile && f.getName.endsWith(".java")).toSet
    requests.foreach { f =>
      if (f.isDirectory) {
        context.spawn(DirectoryAnalyzer(f.getAbsolutePath, searchConfiguration, sourceAnalyzer, context.self, leaderboardActor), f.getAbsolutePath.freshLabel("directory-analyzer"))
      } else if (f.isFile && f.getName.endsWith(".java")) {
        fileAnalyzer ! FileAnalyzer.Count(f.getAbsolutePath, context.self)
      }
    }
    possiblySendAck(
      requests.map(_.getAbsolutePath),
      Report.empty(searchConfiguration),
      Leaderboard(searchConfiguration.numLongestFile),
      sourceAnalyzer,
      parent,
      leaderboardActor,
      path)
  }

  def directoryAnalyzer(
    pendingRequests: Set[String],
    report: Report,
    leaderboard: Leaderboard,
    sourceAnalyzer: ActorRef[SourceAnalyzer.Command],
    parent: ActorRef[DirectoryAnalyzer.Ack],
    leaderboardActor: ActorRef[LeaderboardActor.Command],
    path: String
  ): Behavior[Command] =
    Behaviors.receiveMessage {
      case Result(p, r, l) =>
        val remainingRequest = pendingRequests - p
        val newReport = report merge r
        val newLeaderboard = leaderboard merge l
        possiblySendAck(remainingRequest, newReport, newLeaderboard, sourceAnalyzer, parent, leaderboardActor, path)
      case Ack(p) =>
        val remainingRequest = pendingRequests - p
        possiblySendAck(remainingRequest, report, leaderboard, sourceAnalyzer, parent, leaderboardActor, path)
    }

  private def possiblySendAck(
     pendingRequests: Set[String],
     report: Report,
     leaderboard: Leaderboard,
     sourceAnalyzer: ActorRef[SourceAnalyzer.Command],
     parent: ActorRef[DirectoryAnalyzer.Ack],
     leaderboardActor: ActorRef[LeaderboardActor.Command],
     path: String
   ) = if (pendingRequests.isEmpty) {
    sourceAnalyzer ! SourceAnalyzer.Result(path, report)
    leaderboardActor ! LeaderboardActor.Update(leaderboard)
    parent ! DirectoryAnalyzer.Ack(path)
    Behaviors.stopped
  } else {
    directoryAnalyzer(pendingRequests, report, leaderboard, sourceAnalyzer, parent, leaderboardActor, path)
  }

