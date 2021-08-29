package example.priority

import cats.effect.Async
import cats.effect.std.Queue
import cats.effect.syntax.all._
import cats.syntax.all._

import scala.concurrent.ExecutionContext

object HighLowPriorityRunner {
  final case class Config[F[_]](
      highPriorityJobs: Queue[F, F[Unit]],
      lowPriorityJobs: Queue[F, F[Unit]],
      customEC: Option[ExecutionContext]
  )

  def apply[F[_]](config: Config[F])
                 (implicit F: Async[F]): F[Unit] = {
    val processOneJob =
      config.highPriorityJobs.tryTake.flatMap {
        case Some(hpJob) => hpJob
        case None => config.lowPriorityJobs.tryTake.flatMap {
          case Some(lpJob) => lpJob
          case None => F.unit
        }
      }

    val loop = processOneJob.start.foreverM.void

    config.customEC.fold(ifEmpty = loop)(ec => loop.evalOn(ec))
  }
}
