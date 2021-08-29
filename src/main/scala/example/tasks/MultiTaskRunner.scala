package example.tasks

import cats.effect.IO
import cats.effect.kernel.Ref
import cats.syntax.all._

import scala.concurrent.duration._

object MultiTaskRunner {
  /** Allows a task to report its progress. */
  sealed trait TaskProgressReporter {
    def reportProgress(newProgress: Int): IO[Unit]
  }

  object TaskProgressReporter {
    private[MultiTaskRunner] def fromRef(ref: Ref[IO, Int]): TaskProgressReporter =
      new TaskProgressReporter {
        override def reportProgress(newProgress: Int): IO[Unit] =
          ref.update(_ => newProgress)
      }
  }

  /** A task factory is a tuple of the task name and
   *  a function from its TaskProgressReporter to an IO representing the task work to do.
   */
  type TaskFactory = (String, TaskProgressReporter => IO[Unit])

  /** Runs the provided list of tasks in parallel. */
  def runTasksInParallel(tasksToRun: List[TaskFactory]): IO[Unit] =
    tasksToRun.parTraverse_ {
      case (taskName, taskFactory) =>
        Ref[IO].of(0).flatMap { progressRef =>
          val progressReporter = {
            val reportProgress = progressRef.get.flatMap { taskProgress =>
              IO.println(s"Task (${taskName}): ${taskProgress}%")
            }
            (IO.sleep(1.second) *> reportProgress).foreverM
          }

          progressReporter.background.use { _ =>
            taskFactory(TaskProgressReporter.fromRef(progressRef))
          } *> IO.println(s"Task (${taskName}) finished!")
        }
    }
}
