package com.ruchij

import java.util.concurrent.Executors

import cats.effect.{Blocker, ExitCode, IO, IOApp}
import cats.implicits._
import com.amazonaws.services.lambda.runtime.events.ScheduledEvent
import com.ruchij.lambda.models.MockContext
import com.ruchij.lambda.{GmailVerifierHandler, SendGridHandler}
import com.ruchij.types.FunctionKTypes.fromThrowableEither
import com.ruchij.utils.MonadicUtils
import org.http4s.client.blaze.BlazeClientBuilder
import pureconfig.ConfigSource

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

object App extends IOApp {

  override def run(args: List[String]): IO[ExitCode] = unsafeProgram

  val unsafeProgram: IO[ExitCode] =
    for {
      gmailVerifierHandler <- IO.pure(new GmailVerifierHandler)
      sendGridHandler = new SendGridHandler
      scheduledEvent = new ScheduledEvent

      _ <- IO.delay(sendGridHandler.handleRequest(scheduledEvent, MockContext))

      _ <- MonadicUtils.waitFor[IO](15)

      _ <- IO.delay(gmailVerifierHandler.handleRequest(scheduledEvent, MockContext))
    }
    yield ExitCode.Success

  val program: IO[ExitCode] =
    Blocker.fromExecutorService(IO.delay(Executors.newCachedThreadPool()))
      .product(BlazeClientBuilder[IO](ExecutionContext.global).resource)
      .use {
        case (ioBlocker, client) =>
          for {
            configObjectSource <- IO.delay(ConfigSource.defaultApplication)

            sendGridResult <- SendGridHandler.create[IO](configObjectSource, ioBlocker, client)
            _ <- IO.delay(println(sendGridResult))

            _ <- MonadicUtils.waitFor[IO](15)

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
