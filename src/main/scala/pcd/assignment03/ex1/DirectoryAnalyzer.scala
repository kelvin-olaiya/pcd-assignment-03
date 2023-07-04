package pcd.assignment03.ex1

import akka.actor.typed.{ActorRef, Behavior, DispatcherSelector}
import akka.actor.typed.scaladsl.Behaviors
import pcd.assignment03.ex1.Utils.SearchConfiguration
import Utils.freshLabel

import java.io.File

object DirectoryAnalyzer:
  sealed trait Command
  case class Result(path: String, report: Report, leaderboard: Leaderboard) extends Command

  def apply(
             path: String,
             searchConfiguration: SearchConfiguration,
             replyTo: ActorRef[SourceAnalyzer.Command],
             leaderboardActor: ActorRef[LeaderboardActor.Command]
           ): Behavior[Command] = Behaviors.setup { context =>
    context.log.info(s"Spawned analyzer for directory $path")
    val fileAnalyzer = context.spawn(FileAnalyzer(searchConfiguration), path.freshLabel("file-analyzer"), DispatcherSelector.fromConfig("file-dispatcher"))
    val requests = File(path).listFiles(f => f.isDirectory || f.isFile && f.getName.endsWith("java")).toSet
    requests.foreach { f =>
      if (f.isDirectory) {
        context.spawn(DirectoryAnalyzer(f.getAbsolutePath, searchConfiguration, replyTo, leaderboardActor), f.getAbsolutePath.freshLabel("directory-analyzer"))
      } else if (f.isFile && f.getName.endsWith(".java")) {
        fileAnalyzer ! FileAnalyzer.Count(f.getAbsolutePath, context.self)
      }
    }
    directoryAnalyzer(requests.filter(_.isFile).map(_.getAbsolutePath),
      Report.empty(searchConfiguration),
      Leaderboard(searchConfiguration.numLongestFile),
      replyTo,
      leaderboardActor
    )
  }

  def directoryAnalyzer(
   pendingRequests: Set[String],
   report: Report,
   leaderboard: Leaderboard,
   replyTo: ActorRef[SourceAnalyzer.Command],
   leaderboardActor: ActorRef[LeaderboardActor.Command]
 ): Behavior[Command] = Behaviors.setup { context =>
    Behaviors.receiveMessage {
      case Result(p, r, l) =>
        val pending = pendingRequests - p
        if (pending.isEmpty) {
          replyTo ! SourceAnalyzer.Result(p, report merge r)
          leaderboardActor ! LeaderboardActor.Update(leaderboard merge l)
          //Behaviors.stopped
          Behaviors.same
        } else {
          directoryAnalyzer(pendingRequests - p, report.merge(r), leaderboard, replyTo, leaderboardActor)
        }
    }
  }
