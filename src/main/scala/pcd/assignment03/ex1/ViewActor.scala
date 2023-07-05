package pcd.assignment03.ex1

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors

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
  }

