package pcd.assignment03.ex1.static.controller

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import pcd.assignment03.ex1.dynamic.DirectoryAnalyzer
import pcd.assignment03.ex1.dynamic.Utils.SearchConfiguration
import pcd.assignment03.ex1.dynamic.model.{Leaderboard, Report}
import pcd.assignment03.ex1.{DirectoryAnalyzer, Leaderboard, Report}

import java.io.File
import java.nio.file.Files

object FileAnalyzer:
  sealed trait Command
  case class Count(path: String, replyTo: ActorRef[DirectoryAnalyzer.Command]) extends Command

  def apply(searchConfiguration: SearchConfiguration): Behavior[Command] = Behaviors.receiveMessage {
    case Count(path, replyTo) =>
      var lines = 0
      try {
        lines = Files.lines(File(path).toPath).count().toInt
      } catch { case _: Exception =>} // ignored
      val report = Report(lines, searchConfiguration)
      val leaderboard = Leaderboard(searchConfiguration.numLongestFile).submit(path, lines)
      replyTo ! DirectoryAnalyzer.Result(path, report, leaderboard)
      Behaviors.same
  }

