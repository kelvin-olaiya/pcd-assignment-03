package pcd.assignment03.ex1

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import pcd.assignment03.ex1.ViewActor

object LeaderboardActor:
  sealed trait Command
  case class Update(leaderboard: Leaderboard) extends Command
  //case class Request(replyTo: ActorRef[SourceAnalyzer.Command]) extends Command
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
//      case Request(replyTo) =>
//        replyTo ! SourceAnalyzer.Response(leaderboard)
//        Behaviors.same
      case Init(n) => leaderboardActor(Leaderboard(n), notifyTo)
    }
