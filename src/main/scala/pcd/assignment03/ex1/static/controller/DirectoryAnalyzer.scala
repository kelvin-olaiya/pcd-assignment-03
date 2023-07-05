package pcd.assignment03.ex1.static.controller

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior, DispatcherSelector}
import pcd.assignment03.ex1.static.*
import pcd.assignment03.ex1.static.Utils.SearchConfiguration
import pcd.assignment03.ex1.static.model.{Leaderboard, Report}

import java.io.File

object DirectoryAnalyzer:
  sealed trait Command
  case class Result(path: String, report: Report, leaderboard: Leaderboard) extends Command

  def apply(
    path: String,
    searchConfiguration: SearchConfiguration,
    parent: ActorRef[DirectoryAnalyzer.Result],
  ): Behavior[Command] = Behaviors.setup { context =>
    val fileAnalyzer = context.spawnAnonymous(FileAnalyzer(searchConfiguration), DispatcherSelector.fromConfig("file-dispatcher"))
    var requests = Set[File]()
    try {
      requests = File(path).listFiles(f => f.isDirectory || f.isFile && f.getName.endsWith(".java")).toSet
    } catch { case _: Exception => } //ignored
    requests.foreach { f =>
      if (f.isDirectory) {
        context.spawnAnonymous(DirectoryAnalyzer(f.getAbsolutePath, searchConfiguration, context.self))
      } else if (f.isFile && f.getName.endsWith(".java")) {
        fileAnalyzer ! FileAnalyzer.Count(f.getAbsolutePath, context.self)
      }
    }
    possiblySendAck(
      requests.map(_.getAbsolutePath),
      Report.empty(searchConfiguration),
      Leaderboard(searchConfiguration.numLongestFile),
      parent,
      path)
  }

  def directoryAnalyzer(
    pendingRequests: Set[String],
    report: Report,
    leaderboard: Leaderboard,
    parent: ActorRef[DirectoryAnalyzer.Result],
    path: String
  ): Behavior[Command] =
    Behaviors.receiveMessage {
      case Result(p, r, l) =>
        val remainingRequest = pendingRequests - p
        val newReport = report merge r
        val newLeaderboard = leaderboard merge l
        possiblySendAck(remainingRequest, newReport, newLeaderboard, parent, path)
    }

  private def possiblySendAck(
     pendingRequests: Set[String],
     report: Report,
     leaderboard: Leaderboard,
     parent: ActorRef[DirectoryAnalyzer.Result],
     path: String
   ) = if (pendingRequests.isEmpty) {
    parent ! DirectoryAnalyzer.Result(path, report, leaderboard)
    Behaviors.stopped
  } else {
    directoryAnalyzer(pendingRequests, report, leaderboard, parent, path)
  }

