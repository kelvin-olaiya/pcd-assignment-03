package pcd.assignment03.ex1.dynamic.view

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import pcd.assignment03.ex1.dynamic.view.View
import pcd.assignment03.ex1.dynamic.model.{Leaderboard, Report}

object ViewActor:
  sealed trait Command
  case class SetView(view: View) extends Command
  case class Stopped() extends Command
  case class UpdateLeaderboard(leaderboard: Leaderboard) extends Command
  case class UpdateReport(report: Report) extends Command

  def apply(): Behavior[Command] = idle()

  private def idle(): Behavior[Command] = Behaviors.receiveMessage {
    case SetView(view) => observing(view)
  }

  private def observing(view: View): Behavior[Command] = Behaviors.receiveMessage {
    case UpdateLeaderboard(l) =>
      view.updateLeaderboard(l)
      Behaviors.same
    case UpdateReport(r) =>
      view.updateReport(r)
      Behaviors.same
    case Stopped() =>
      view.terminated()
      Behaviors.same
    case SetView(view) => observing(view)
  }

