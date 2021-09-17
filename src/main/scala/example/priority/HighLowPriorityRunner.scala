package example.priority

import cats.effect.IO
import cats.effect.std.Queue

import scala.concurrent.ExecutionContext

object HighLowPriorityRunner {
  final case class Config(
      highPriorityJobs: Queue[IO, IO[Unit]],
      lowPriorityJobs: Queue[IO, IO[Unit]],
      customEC: Option[ExecutionContext]
  )

  def apply(config: Config): IO[Unit] = {
    val processOneJob =
      config.highPriorityJobs.tryTake.flatMap {
        case Some(hpJob) => hpJob
        case None => config.lowPriorityJobs.tryTake.flatMap {
          case Some(lpJob) => lpJob
          case None => IO.unit
        }
      }

    val loop = processOneJob.start.foreverM.void

    config.customEC.fold(ifEmpty = loop)(ec => loop.evalOn(ec))
  }
}
