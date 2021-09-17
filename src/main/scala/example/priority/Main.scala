package example.priority

import cats.effect.{IO, IOApp, Resource}
import cats.effect.std.Queue
import cats.syntax.all._

import java.util.concurrent.Executors
import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

object Main extends IOApp.Simple {
  private def createJob(id: Int): IO[Unit] =
    IO.delay(println(s"Starting job ${id} on thread ${Thread.currentThread.getName}")) >> // Use Console over println on real code.
    IO.delay(Thread.sleep(1.second.toMillis)) >> // Blocks the Fiber! - Only for testing, use F.sleep on real code.
    IO.delay(println(s"Finished job ${id}!"))

  private def program(customEC: ExecutionContext): IO[Unit] = for {
    highPriorityJobs <- Queue.unbounded[IO, IO[Unit]]
    lowPriorityJobs <- Queue.unbounded[IO, IO[Unit]]
    runnerFiber <- HighLowPriorityRunner(HighLowPriorityRunner.Config(
      highPriorityJobs,
      lowPriorityJobs,
      Some(customEC)
    )).start
    _ <- List.range(0, 10).traverse_(id => highPriorityJobs.offer(createJob(id)))
    _ <- List.range(10, 15).traverse_(id => lowPriorityJobs.offer(createJob(id)))
    _ <- IO.sleep(5.seconds)
    _ <- List.range(15, 20).traverse_(id => highPriorityJobs.offer(createJob(id)))
    _ <- runnerFiber.join.void
  } yield ()

  override final val run: IO[Unit] =
    Resource.make(IO(Executors.newFixedThreadPool(2)))(ec => IO.blocking(ec.shutdown())).use { ec =>
      program(ExecutionContext.fromExecutor(ec))
    }
}
