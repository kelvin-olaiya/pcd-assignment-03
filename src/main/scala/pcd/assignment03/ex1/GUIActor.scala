package pcd.assignment03.ex1

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors

object GUIActor:
  sealed trait Command
  case class Stopped() extends Command
  case class UpdateLeaderboard(leaderboard: Leaderboard) extends Command
  case class UpdateReport(report: Report) extends Command

  def apply(gui: GUI): Behavior[Command] = GUIActor(gui)
  private def GUIActor(gui: GUI): Behavior[Command] =
    Behaviors.receiveMessage {
      case UpdateLeaderboard(l) =>
        gui.updateLeaderboard(l)
        Behaviors.same
      case UpdateReport(r) =>
        gui.updateReport(r)
        Behaviors.same
      case Stopped() =>
        gui.stopCounting()
        Behaviors.same
    }

