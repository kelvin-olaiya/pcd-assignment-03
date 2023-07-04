package pcd.assignment03.ex1

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import pcd.assignment03.ex1.Utils.SearchConfiguration

import java.io.File
import java.nio.file.Files

object FileAnalyzer:
  sealed trait Command
  case class Count(path: String, replyTo: ActorRef[DirectoryAnalyzer.Command]) extends Command

  def apply(searchConfiguration: SearchConfiguration): Behavior[Command] = Behaviors.receiveMessage {
    case Count(path, replyTo) =>
      val lines = Files.lines(File(path).toPath).count().toInt
      val report = Report(lines, searchConfiguration)
      val leaderboard = Leaderboard(searchConfiguration.numLongestFile).submit(path, lines)
      replyTo ! DirectoryAnalyzer.Result(path, report, leaderboard)
      Behaviors.same
  }

