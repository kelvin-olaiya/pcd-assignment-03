package pcd.assignment03.ex1.static

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, ActorSystem, Behavior, DispatcherSelector}
import pcd.assignment03.ex1.*
import pcd.assignment03.ex1.static.Utils.SearchConfiguration
import pcd.assignment03.ex1.static.controller.DirectoryAnalyzer
import pcd.assignment03.ex1.static.model.{Leaderboard, Report}

import java.io.File
import java.nio.file.{Files, Paths}
import scala.collection.immutable.TreeSet
import scala.concurrent.ExecutionContext

object Manager:
  sealed trait Command
  case class Start(path: String, searchConfiguration: SearchConfiguration) extends Command
  private case class AdaptedResult(result: DirectoryAnalyzer.Result) extends Command

  def apply(): Behavior[Command] = Behaviors.setup { context =>
    val ackResponseAdapter: ActorRef[DirectoryAnalyzer.Result] = context.messageAdapter(rsp => AdaptedResult(rsp))
    idle(ackResponseAdapter)
  }

  private def idle(resultAdapter: ActorRef[DirectoryAnalyzer.Result]): Behavior[Command] = Behaviors.setup { context =>
    Behaviors.receiveMessage {
      case Start(p, s) =>
        context.spawnAnonymous(DirectoryAnalyzer(p, s, resultAdapter))
        working()
    }
  }

  private def working(): Behavior[Command] = Behaviors.receiveMessage {
      case AdaptedResult(r) =>
        println("Distribution: \n" + r.report)
        println("Leaderboard: \n" + r.leaderboard)
        Behaviors.stopped
    }


object Main extends App:
  val system = ActorSystem(Manager(), "manager")

  println("Inserisci percorso: ")
  val path = scala.io.StdIn.readLine()

  println("Estremo massimo: ")
  val max = scala.io.StdIn.readInt()

  println("Numero intervalli: ")
  val numIntervals = scala.io.StdIn.readInt()

  println("Numero classificati: ")
  val numFinalists = scala.io.StdIn.readInt()

  val searchConfiguration: SearchConfiguration = SearchConfiguration(max, numIntervals, numFinalists)

  println("----------------")

  private var report = Report(searchConfiguration.maxLines, searchConfiguration.numIntervals)
  private var leaderboard = Leaderboard(searchConfiguration.numLongestFile)

  system ! Manager.Start(path, searchConfiguration)