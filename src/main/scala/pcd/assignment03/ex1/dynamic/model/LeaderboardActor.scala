package pcd.assignment03.ex1.dynamic.model

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import pcd.assignment03.ex1.dynamic.model.Leaderboard
import pcd.assignment03.ex1.dynamic.view.ViewActor

object LeaderboardActor:
  sealed trait Command
  case class Update(leaderboard: Leaderboard) extends Command
  case class Init(numLongestFiles: Int) extends Command

  def apply(notifyTo: ActorRef[ViewActor.Command]): Behavior[Command] = idle(notifyTo)
    

  private def idle(notifyTo: ActorRef[ViewActor.Command]): Behavior[Command] = Behaviors.receiveMessage {
    case Init(n) => leaderboardActor(Leaderboard(n), notifyTo)
  }
  private def leaderboardActor(leaderboard: Leaderboard, notifyTo: ActorRef[ViewActor.Command]): Behavior[Command] =
    Behaviors.receiveMessage {
      case Update(l) =>
        val newLeaderboard = leaderboard merge l
        notifyTo ! ViewActor.UpdateLeaderboard(newLeaderboard)
        leaderboardActor(newLeaderboard, notifyTo)

      case Init(n) => leaderboardActor(Leaderboard(n), notifyTo)
    }
