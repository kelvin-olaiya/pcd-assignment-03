package pcd.assignment03.ex1.dynamic

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, ActorSystem, Behavior, DispatcherSelector}
import pcd.assignment03.ex1.dynamic.Manager.manager
import pcd.assignment03.ex1.dynamic.model.Report.*
import pcd.assignment03.ex1.dynamic.Utils.*
import pcd.assignment03.ex1.*
import pcd.assignment03.ex1.dynamic.controller.SourceAnalyzer
import pcd.assignment03.ex1.dynamic.model.LeaderboardActor
import pcd.assignment03.ex1.dynamic.view.{CLI, GUI, View, ViewActor}

import java.io.File
import java.nio.file.{Files, Paths}
import scala.collection.immutable.TreeSet
import scala.concurrent.ExecutionContext

object Manager:
  sealed trait Command
  case class Start(path: String, searchConfiguration: SearchConfiguration, view: View) extends Command
  case class Stop() extends Command
  case class Completed() extends Command

  def apply(): Behavior[Command] = Behaviors.setup { context =>
    val viewActor = context.spawnAnonymous(ViewActor())
    val leaderboardActor = context.spawn(LeaderboardActor(viewActor), "leaderboard-actor")
    idle(viewActor, leaderboardActor)
  }

  private def idle(
    viewActor: ActorRef[ViewActor.Command],
    leaderboardActor: ActorRef[LeaderboardActor.Command]
  ): Behavior[Command] = Behaviors.setup { context =>
    Behaviors.receiveMessage {
      case Start(p, s, v) =>
        viewActor ! ViewActor.SetView(v)
        val sourceAnalyzer = context.spawn(SourceAnalyzer(s, viewActor, leaderboardActor, context.self), "source-analyzer")
        sourceAnalyzer ! SourceAnalyzer.Count(p)
        manager(sourceAnalyzer, viewActor, leaderboardActor)
    }
  }

  private def manager(
    sourceAnalyzer: ActorRef[SourceAnalyzer.Command],
    viewActor: ActorRef[ViewActor.Command],
    leaderboardActor: ActorRef[LeaderboardActor.Command]
  ): Behavior[Command] =
    Behaviors.receiveMessage {
      case Stop() =>
        sourceAnalyzer ! SourceAnalyzer.Halt()
        idle(viewActor, leaderboardActor)
      case Completed() =>
        idle(viewActor, leaderboardActor)
    }

object Main extends App:
  val system = ActorSystem(Manager(), "manager")
  println("Seleziona modalità: \n 1) CLI \n 2) GUI")
  val mode = scala.io.StdIn.readLine()
  mode match
    case "1" => CLI(system)
    case "2" => GUI(system)