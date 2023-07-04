package pcd.assignment03.ex1

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, ActorSystem, Behavior, DispatcherSelector}
import pcd.assignment03.ex1.Report.*
import pcd.assignment03.ex1.Utils.*
import pcd.assignment03.ex1.Utils.freshLabel
import pcd.assignment03.ex1.GUI

import java.io.File
import java.nio.file.{Files, Paths}
import scala.collection.immutable.TreeSet
import scala.concurrent.ExecutionContext
import Utils.freshLabel

object Manager:
  sealed trait Command
  case class Start(path: String, searchConfiguration: SearchConfiguration) extends Command
  case class Stop() extends Command

  def apply(gui: GUI): Behavior[Command] = Behaviors.setup { context =>
    Behaviors.receiveMessage {
      case Start(p, s) =>
        val sourceAnalyzer = context.spawn(SourceAnalyzer(s, gui), "source-analyzer")
        sourceAnalyzer ! SourceAnalyzer.Count(p)
        manager(sourceAnalyzer, gui)
    }
  }

  private def manager(sourceAnalyzer: ActorRef[SourceAnalyzer.Command], gui: GUI): Behavior[Command] = Behaviors.receiveMessage {
    case Stop() =>
      sourceAnalyzer ! SourceAnalyzer.Halt()
      Manager(gui)
  }

object Main extends App:
  GUI()