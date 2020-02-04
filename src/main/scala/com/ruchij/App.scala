package com.ruchij

import java.util.concurrent.Executors

import cats.effect.{Blocker, ExitCode, IO, IOApp}
import com.ruchij.lambda.{GmailVerifierHandler, SendGridHandler}
import com.ruchij.types.FunctionKTypes.fromThrowableEither
import fs2.Stream
import pureconfig.ConfigSource

import scala.concurrent.duration._
import scala.language.postfixOps

object App extends IOApp {

  override def run(args: List[String]): IO[ExitCode] =
    Blocker.fromExecutorService(IO.delay(Executors.newCachedThreadPool()))
      .use {
        ioBlocker =>
          for {
            configObjectSource <- IO.delay(ConfigSource.defaultApplication)

            sendGridResult <- SendGridHandler.create[IO](configObjectSource, ioBlocker)
            _ <- IO.delay(println(sendGridResult))

            _ <-
              Stream.range[IO](20, 0, -5)
                .evalMap(count => IO.delay(println(s"$count seconds to go...")))
                .evalMap(_ => IO.sleep(5 second))
                .compile
                .drain

            _ <- IO.delay(println("Waiting COMPLETED"))

            verificationResult <- GmailVerifierHandler.create[IO](configObjectSource, ioBlocker)
            _ <- IO.delay(println(verificationResult))
          }
          yield ExitCode.Success
      }
}
