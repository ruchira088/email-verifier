package com.ruchij

import java.util.concurrent.Executors

import cats.effect.{Blocker, ExitCode, IO, IOApp}
import com.github.javafaker.Faker
import com.ruchij.config.GmailConfiguration
import com.ruchij.lambda.SendGridHandler
import com.ruchij.services.email.models.Email
import com.ruchij.services.gmail.GmailServiceImpl
import com.ruchij.services.joke.JokeService
import com.ruchij.types.FunctionKTypes.fromThrowableEither
import pureconfig.ConfigSource

object App extends IOApp {

  override def run(args: List[String]): IO[ExitCode] =
    Blocker.fromExecutorService(IO.delay(Executors.newCachedThreadPool()))
      .use {
        ioBlocker =>
          for {
            configObjectSource <- IO.delay(ConfigSource.defaultApplication)

//            sendGridResponse <-
//              SendGridHandler.handle(ioBlocker, configObjectSource, new JokeService[IO](Faker.instance()))
//
//            _ <- IO.delay(println(sendGridResponse.getStatusCode))

            gmailConfiguration <- GmailConfiguration.load[IO](configObjectSource)

            gmailService <- GmailServiceImpl.create[IO](gmailConfiguration, ioBlocker)

            response <- gmailService.fetchMessages(gmailConfiguration.sender, None)

            _ <- IO.delay(println(response))
          }
          yield ExitCode.Success
      }
}
