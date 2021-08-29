package example.tasks

import cats.effect.{IO, IOApp}
import cats.syntax.all._

import scala.concurrent.duration._

object Main extends IOApp.Simple {
  override final val run: IO[Unit] =
    MultiTaskRunner.runTasksInParallel(List(
      "Task one" -> { reporter =>
        List.range(start = 1, end = 11).traverse_ { i =>
          IO.sleep(2.seconds) *> reporter.reportProgress(newProgress = i * 10)
        }
      },
      "Task two" -> { reporter =>
        List.range(start = 1, end = 4).traverse_ { i =>
          IO.sleep(3.seconds) *> reporter.reportProgress(newProgress = i * 33)
        }
      },
      "Task three" -> { reporter =>
        List.range(start = 1, end = 21).traverse_ { i =>
          IO.sleep(500.milliseconds) *> reporter.reportProgress(newProgress = i * 5)
        }
      }
    ))
}
