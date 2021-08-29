package example.priority

import cats.effect.{Async, IO, IOApp, Resource}
import cats.effect.std.Queue
import cats.effect.syntax.all._
import cats.syntax.all._

import java.util.concurrent.Executors
import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

object Main extends IOApp.Simple {
  override final val run: IO[Unit] =
    Resource.make(IO(Executors.newFixedThreadPool(2)))(ec => IO.blocking(ec.shutdown())).use { ec =>
      Program[IO](ExecutionContext.fromExecutor(ec))
    }
}

object Program {
  private def createJob[F[_]](id: Int)(implicit F: Async[F]): F[Unit] =
    F.delay(println(s"Starting job ${id} on thread ${Thread.currentThread.getName}")) *> // Use Console over println on real code.
    F.delay(Thread.sleep(1.second.toMillis)) *> // Blocks the Fiber! - Only for testing, use F.sleep on real code.
    F.delay(println(s"Finished job ${id}!"))

  def apply[F[_]](customEC: ExecutionContext)(implicit F: Async[F]): F[Unit] = for {
    highPriorityJobs <- Queue.unbounded[F, F[Unit]]
    lowPriorityJobs <- Queue.unbounded[F, F[Unit]]
    runnerFiber <- HighLowPriorityRunner(HighLowPriorityRunner.Config(
      highPriorityJobs,
      lowPriorityJobs,
      Some(customEC)
    )).start
    _ <- List.range(0, 10).traverse_(id => highPriorityJobs.offer(createJob(id)))
    _ <- List.range(10, 15).traverse_(id => lowPriorityJobs.offer(createJob(id)))
    _ <- F.sleep(5.seconds)
    _ <- List.range(15, 20).traverse_(id => highPriorityJobs.offer(createJob(id)))
    _ <- runnerFiber.join.void
  } yield ()
}
