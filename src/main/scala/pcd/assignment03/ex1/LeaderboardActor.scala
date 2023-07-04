package pcd.assignment03.ex1

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.Behaviors

object LeaderboardActor:
  sealed trait Command
  case class Update(leaderboard: Leaderboard) extends Command
  case class Request(replyTo: ActorRef[SourceAnalyzer.Command]) extends Command

  def apply(numLongestFiles: Int, notifyTo: ActorRef[GUIActor.Command]): Behavior[Command] =
    leaderboardActor(Leaderboard(numLongestFiles), notifyTo)

  private def leaderboardActor(leaderboard: Leaderboard, notifyTo: ActorRef[GUIActor.Command]): Behavior[Command] =
    Behaviors.receiveMessage {
      case Update(l) =>
        notifyTo ! GUIActor.UpdateLeaderboard(leaderboard merge l)
        leaderboardActor(leaderboard merge l, notifyTo)
      case Request(replyTo) =>
        replyTo ! SourceAnalyzer.Response(leaderboard)
        Behaviors.same
    }
