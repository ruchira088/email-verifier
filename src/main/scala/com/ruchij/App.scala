package com.ruchij

import java.util.concurrent.Executors

import cats.effect.{Blocker, ExitCode, IO, IOApp}
import com.ruchij.config.ApplicationConfiguration
import com.ruchij.services.email.models.Email
import com.ruchij.services.gmail.GmailServiceImpl
import com.ruchij.types.FunctionKTypes.fromThrowableEither
import pureconfig.ConfigSource

object App extends IOApp {

  override def run(args: List[String]): IO[ExitCode] =
    Blocker.fromExecutorService(IO.delay(Executors.newCachedThreadPool()))
      .use {
        ioBlocker =>
          for {
            configObjectSource <- IO.delay(ConfigSource.defaultApplication)

            applicationConfiguration <- ApplicationConfiguration.load[IO](configObjectSource)

            gmailService <- GmailServiceImpl.create[IO](applicationConfiguration.gmailConfiguration, ioBlocker)

            response <- gmailService.fetchMessages(Email.lift("ruchira.jayasekara@skyfii.com"), None)

            _ <- IO.delay(println(response))
          }
          yield ExitCode.Success
      }
}
