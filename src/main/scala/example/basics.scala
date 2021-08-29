import cats.effect.{IO, IOApp, Resource}
import cats.syntax.all._

import scala.concurrent.duration._ // Provides duration units.
import scala.io.Source


/** Program 1: Sequential composition. */
object Program1_1 extends IOApp.Simple {
  val myIO: IO[Unit] = IO {
    println("Hello, World!")
  }

  override final val run: IO[Unit] =
    myIO.flatMap(_ => myIO)
}

object Program1_2 extends IOApp.Simple {
  val myIO: IO[Unit] = IO {
    println("Hello, World!")
  }

  override final val run: IO[Unit] =
    for {
      _ <- myIO
      _ <- myIO
    } yield ()
}

object Program1_3 extends IOApp.Simple {
  val myIO: IO[Unit] = IO {
    println("Hello, World!")
  }

  override final val run: IO[Unit] =
    myIO >> myIO
}
// ----------------------------------------------


/** Program 2: Async & Cancellable operations. */
object Program2 extends IOApp.Simple {
  val tick: IO[Unit] =
    (IO.sleep(1.second) >> IO.println("Tick")).foreverM

  val cancel: IO[Unit] =
    IO.readLine.void

  override final val run: IO[Unit] =
    for {
      tickFiber <- tick.start
      _ <- cancel
      _ <- tickFiber.cancel
    } yield ()
}
// ----------------------------------------------


/** Program 3: Parallel operations. */
object Program3_1 extends IOApp.Simple {
  val ioA: IO[Int] =
    IO.sleep(1.second) >> IO(println("Running ioA")).as(1)

  val ioB: IO[Int] =
    IO.sleep(1.second) >> IO(println("Running ioB")).as(2)

  val ioC: IO[Int] =
    IO.sleep(1.second) >> IO(println("Running ioC")).as(3)

  override final val run: IO[Unit] =
    (ioA, ioB, ioC).parTupled.flatMap {
      case (a, b, c) =>
        IO.println(s"a: ${a} | b: ${b} | c: ${c}")
    }
}

object Program3_2 extends IOApp.Simple {
  override final val run: IO[Unit] =
    (1 to 10).toList.parTraverse { i =>
      IO.sleep(1.second) >> IO.println(i) >> IO.delay((i * 10) + 5)
    } flatMap { data =>
      IO.println(s"Final data: ${data}")
    }
}
// ----------------------------------------------


/** Program 4: Error handling and resource management. */
object Program4 extends IOApp.Simple {
  def fahrenheitToCelsius(f: Double): Double =
    (f - 32.0) * (5.0 / 9.0)

  def process(lines: List[String]): List[Double] =
    for {
      line <- lines
      if (!line.trim.isEmpty && !line.startsWith("//"))
      fahrenheit = line.toDouble
      celsius = fahrenheitToCelsius(fahrenheit)
    } yield celsius

  override final val run: IO[Unit] =
    Resource
      .fromAutoCloseable(IO(Source.fromFile("fahrenheit.txt")))
      .use(file => IO(file.getLines().toList))
      .map(process)
      .attempt
      .flatMap {
        case Right(data) =>
          IO.println(s"Output: ${data.take(5).mkString("[", ", ", ", ...]")}")

        case Left(ex) =>
          IO.println(s"Error: ${ex.getMessage}")
      }
}
// ----------------------------------------------
