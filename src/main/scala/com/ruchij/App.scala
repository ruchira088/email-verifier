package com.ruchij

import java.util.concurrent.Executors

import cats.effect.{Blocker, ExitCode, IO, IOApp}
import cats.implicits._
import com.ruchij.lambda.{GmailVerifierHandler, SendGridHandler}
import com.ruchij.types.FunctionKTypes.fromThrowableEither
import com.ruchij.utils.MonadicUtils
import org.http4s.client.blaze.BlazeClientBuilder
import pureconfig.ConfigSource

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

object App extends IOApp {

  override def run(args: List[String]): IO[ExitCode] =
    Blocker.fromExecutorService(IO.delay(Executors.newCachedThreadPool()))
      .product(BlazeClientBuilder[IO](ExecutionContext.global).resource)
      .use {
        case (ioBlocker, client) =>
          for {
            configObjectSource <- IO.delay(ConfigSource.defaultApplication)

            sendGridResult <- SendGridHandler.create[IO](configObjectSource, ioBlocker, client)
            _ <- IO.delay(println(sendGridResult))

            verificationResult <-
              MonadicUtils.retryWithDelay(
                GmailVerifierHandler.create[IO](configObjectSource, ioBlocker, client),
                5 seconds,
                20
              )

            _ <- IO.delay(println(verificationResult))
          }
          yield ExitCode.Success
      }
}
