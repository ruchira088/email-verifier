package com.ruchij.lambda

import java.util.concurrent.TimeUnit

import cats.effect.{Blocker, Clock, ContextShift, Sync}
import cats.implicits._
import cats.~>
import com.ruchij.config.SendGridConfiguration
import com.ruchij.services.email.SendGridEmailService
import com.ruchij.services.email.models.Email
import com.ruchij.services.joke.JokeService
import com.sendgrid.{Response, SendGrid}
import html.VerificationEmail
import org.joda.time.DateTime
import pureconfig.ConfigObjectSource

object SendGridHandler {

  def handle[F[_]: Sync: Either[Throwable, *] ~> *[_]: ContextShift: Clock](
    blocker: Blocker,
    configObjectSource: ConfigObjectSource,
    jokeService: JokeService[F]
  ): F[Response] =
    for {
      sendGridConfiguration <- SendGridConfiguration.load[F](configObjectSource)

      sendGridService = new SendGridEmailService[F](new SendGrid(sendGridConfiguration.apiKey), blocker)

      timestamp <- Clock[F].realTime(TimeUnit.MILLISECONDS)

      joke <- jokeService.joke

      response <- sendGridService.send {
        Email(
          sendGridConfiguration.destination,
          sendGridConfiguration.sender,
          s"Verification Email at ${new DateTime(timestamp)}",
          VerificationEmail(new DateTime(timestamp), joke).body
        )
      }

    } yield response
}
