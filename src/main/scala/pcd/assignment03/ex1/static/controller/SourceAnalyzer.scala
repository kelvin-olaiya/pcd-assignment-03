package pcd.assignment03.ex1.static.controller

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import akka.pattern.Patterns
import pcd.assignment03.ex1.dynamic.Utils.SearchConfiguration
import pcd.assignment03.ex1.dynamic.controller.DirectoryAnalyzer
import pcd.assignment03.ex1.dynamic.model.{Leaderboard, LeaderboardActor, Report}
import pcd.assignment03.ex1.dynamic.view.ViewActor
import pcd.assignment03.ex1.dynamic.{Manager, controller}
import pcd.assignment03.ex1.*

import java.io.File
import scala.concurrent.Await
import scala.concurrent.duration.*

object SourceAnalyzer:
  import pcd.assignment03.ex1.dynamic.model.Leaderboard.*
  sealed trait Command
  case class Count(path: String) extends Command
  case class Result(path: String, report: Report) extends Command
  case class Response(leaderboard: Leaderboard) extends Command
  case class Halt() extends Command
  private case class AdaptedAck(ack: static.controller.DirectoryAnalyzer.Ack) extends Command

  def apply(
   config: SearchConfiguration,
   viewActor: ActorRef[ViewActor.Command],
   leaderboardActor: ActorRef[LeaderboardActor.Command],
   manager: ActorRef[Manager.Completed],
  ): Behavior[Command] =
    Behaviors.setup { context =>
      val ackResponseAdapter: ActorRef[static.controller.DirectoryAnalyzer.Ack] = context.messageAdapter(rsp => AdaptedAck(rsp))
      Behaviors.receiveMessage {
        case Count(path) =>
          leaderboardActor ! LeaderboardActor.Init(config.numLongestFile)
          context.spawnAnonymous(controller.DirectoryAnalyzer(path, config, context.self, ackResponseAdapter, leaderboardActor))
          analyzeBehavior(Report(config.maxLines, config.numIntervals), config, path, viewActor, leaderboardActor, manager)
      }
    }

  private def analyzeBehavior(
     report: Report,
     searchConfig: SearchConfiguration,
     rootPath: String,
     viewActor: ActorRef[ViewActor.Command],
     leaderboardActor: ActorRef[LeaderboardActor.Command],
     manager: ActorRef[Manager.Completed]
   ): Behavior[Command] =
    Behaviors.receiveMessage {
      case Result(_, r) =>
        val newReport = report merge r
        viewActor ! ViewActor.UpdateReport(newReport)
        analyzeBehavior(newReport, searchConfig, rootPath, viewActor, leaderboardActor, manager)
      case AdaptedAck(_) =>
        viewActor ! ViewActor.Stopped()
        manager ! Manager.Completed()
        Behaviors.stopped
      case Halt() =>
        viewActor ! ViewActor.Stopped()
        Behaviors.stopped
    }

