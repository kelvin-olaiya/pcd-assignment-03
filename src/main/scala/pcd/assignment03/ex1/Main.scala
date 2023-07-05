package pcd.assignment03.ex1

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, ActorSystem, Behavior, DispatcherSelector}
import pcd.assignment03.ex1.Report.*
import pcd.assignment03.ex1.Utils.*
import pcd.assignment03.ex1.GUI

import java.io.File
import java.nio.file.{Files, Paths}
import scala.collection.immutable.TreeSet
import scala.concurrent.ExecutionContext
import pcd.assignment03.ex1.Manager.manager

object Manager:
  sealed trait Command
  case class Start(path: String, searchConfiguration: SearchConfiguration) extends Command
  case class Stop() extends Command
  case class Completed() extends Command

  def apply(gui: GUI): Behavior[Command] = Behaviors.setup { context =>
    val guiActor = context.spawnAnonymous(GUIActor(gui))
    val leaderboardActor = context.spawn(LeaderboardActor(guiActor), "leaderboard-actor")
    idle(guiActor, leaderboardActor)
  }

  private def idle(
    guiActor: ActorRef[GUIActor.Command],
    leaderboardActor: ActorRef[LeaderboardActor.Command]
  ): Behavior[Command] = Behaviors.setup { context =>
    Behaviors.receiveMessage {
      case Start(p, s) =>
        val sourceAnalyzer = context.spawn(SourceAnalyzer(s, guiActor, leaderboardActor, context.self), "source-analyzer")
        sourceAnalyzer ! SourceAnalyzer.Count(p)
        manager(sourceAnalyzer, guiActor, leaderboardActor)
    }
  }

  private def manager(
    sourceAnalyzer: ActorRef[SourceAnalyzer.Command],
    guiActor: ActorRef[GUIActor.Command],
    leaderboardActor: ActorRef[LeaderboardActor.Command]
  ): Behavior[Command] =
    Behaviors.receiveMessage {
      case Stop() =>
        sourceAnalyzer ! SourceAnalyzer.Halt()
        idle(guiActor, leaderboardActor)
      case Completed() =>
        idle(guiActor, leaderboardActor)
    }

object Main extends App:
  GUI()