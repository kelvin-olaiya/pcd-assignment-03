package pcd.assignment03.ex1

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import pcd.assignment03.ex1.Utils.SearchConfiguration
import Utils.freshLabel

import java.io.File

object SourceAnalyzer:
  import Leaderboard.*
  sealed trait Command
  case class Count(path: String) extends Command
  case class Result(path: String, report: Report) extends Command
  case class Response(leaderboard: Leaderboard) extends Command
  case class Halt() extends Command
  private case class AdaptedAck(ack: DirectoryAnalyzer.Ack) extends Command

  def apply(config: SearchConfiguration, gui: GUI): Behavior[Command] =
    Behaviors.setup { context =>
      val backendResponseMapper: ActorRef[DirectoryAnalyzer.Ack] = context.messageAdapter(rsp => AdaptedAck(rsp))
      Behaviors.receiveMessage {
        case Count(path) =>
          context.log.info(s"Spawning source analyzer for path $path")
          val guiActor = context.spawn(GUIActor(gui), "gui-actor")
          val leaderboardActor = context.spawn(LeaderboardActor(config.numLongestFile, guiActor), "leaderboard-actor")
          context.spawn(DirectoryAnalyzer(path, config, context.self, backendResponseMapper, leaderboardActor), File(path).getAbsolutePath.freshLabel("directory-analyzer"))
          analyzeBehavior(Report(config.maxLines, config.numIntervals), config, path, guiActor)
      }
    }

  private def analyzeBehavior(
     report: Report,
     searchConfig: SearchConfiguration,
     rootPath: String,
     guiActor: ActorRef[GUIActor.Command]
   ): Behavior[Command] =
    Behaviors.receiveMessage {
      case Result(_, r) =>
        val newReport = report merge r
        guiActor ! GUIActor.UpdateReport(newReport)
        analyzeBehavior(newReport, searchConfig, rootPath, guiActor)
      case AdaptedAck(ack) =>
        Behaviors.stopped
      case Halt() =>
        Behaviors.stopped
    }

