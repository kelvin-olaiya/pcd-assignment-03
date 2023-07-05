package pcd.assignment03.ex1

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import akka.pattern.Patterns
import pcd.assignment03.ex1.Utils.SearchConfiguration
import pcd.assignment03.ex1.ViewActor

import java.io.File
import scala.concurrent.Await
import scala.concurrent.duration._

object SourceAnalyzer:
  import Leaderboard.*
  sealed trait Command
  case class Count(path: String) extends Command
  case class Result(path: String, report: Report) extends Command
  case class Response(leaderboard: Leaderboard) extends Command
  case class Halt() extends Command
  private case class AdaptedAck(ack: DirectoryAnalyzer.Ack) extends Command

  def apply(
   config: SearchConfiguration,
   viewActor: ActorRef[ViewActor.Command],
   leaderboardActor: ActorRef[LeaderboardActor.Command],
   manager: ActorRef[Manager.Completed],
  ): Behavior[Command] =
    Behaviors.setup { context =>
      val ackResponseAdapter: ActorRef[DirectoryAnalyzer.Ack] = context.messageAdapter(rsp => AdaptedAck(rsp))
      Behaviors.receiveMessage {
        case Count(path) =>
          leaderboardActor ! LeaderboardActor.Init(config.numLongestFile)
          context.spawnAnonymous(DirectoryAnalyzer(path, config, context.self, ackResponseAdapter, leaderboardActor))
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

