package pcd.assignment03.ex1

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.Behaviors

object LeaderboardActor:
  sealed trait Command
  case class Update(leaderboard: Leaderboard) extends Command
  case class Request(replyTo: ActorRef[SourceAnalyzer.Command]) extends Command
  case class Init(numLongestFiles: Int) extends Command

  def apply(notifyTo: ActorRef[GUIActor.Command]): Behavior[Command] = idle(notifyTo)
    

  private def idle(notifyTo: ActorRef[GUIActor.Command]): Behavior[Command] = Behaviors.receiveMessage {
    case Init(n) => leaderboardActor(Leaderboard(n), notifyTo)
  }
  private def leaderboardActor(leaderboard: Leaderboard, notifyTo: ActorRef[GUIActor.Command]): Behavior[Command] =
    Behaviors.receiveMessage {
      case Update(l) =>
        val newLeaderboard = leaderboard merge l
        notifyTo ! GUIActor.UpdateLeaderboard(newLeaderboard)
        leaderboardActor(newLeaderboard, notifyTo)
      case Request(replyTo) =>
        replyTo ! SourceAnalyzer.Response(leaderboard)
        Behaviors.same
      case Init(n) => leaderboardActor(Leaderboard(n), notifyTo)
    }
