package com.ruchij.lambda

import cats.effect.{Blocker, Clock, ContextShift, Sync}
import cats.implicits._
import cats.{Monad, ~>}
import com.github.javafaker.Faker
import com.ruchij.config.{SendGridConfiguration, VerificationConfiguration}
import com.ruchij.services.email.models.Email
import com.ruchij.services.email.models.Email.EmailAddress
import com.ruchij.services.email.{EmailService, SendGridEmailService}
import com.ruchij.services.joke.{ChuckNorrisJokeService, JokeService, LocalJokeService}
import com.ruchij.services.verify.VerificationService
import com.sendgrid.SendGrid
import html.VerificationEmail
import org.http4s.client.Client
import pureconfig.ConfigObjectSource

object SendGridHandler {

  def create[F[_]: Sync: ContextShift: Clock: Either[Throwable, *] ~> *[_]](
    configObjectSource: ConfigObjectSource,
    blocker: Blocker,
    client: Client[F]
  ): F[EmailService[F]#Response] =
    for {
      sendGridConfiguration <- SendGridConfiguration.load[F](configObjectSource)
      verificationConfiguration <- VerificationConfiguration.load[F](configObjectSource)

      sendGridEmailService = new SendGridEmailService[F](new SendGrid(sendGridConfiguration.apiKey), blocker)
      verificationService = new VerificationService[F](verificationConfiguration.messagePeriod)

      chuckNorrisJokeService = new ChuckNorrisJokeService[F](client)

      result <- run(sendGridEmailService, verificationService, chuckNorrisJokeService)(
        sendGridConfiguration.destination,
        sendGridConfiguration.sender
      )
    } yield result

  def run[F[_]: Monad](
    emailService: EmailService[F],
    verificationService: VerificationService[F],
    jokeService: JokeService[F]
  )(destination: EmailAddress, sender: EmailAddress): F[emailService.Response] =
    for {
      joke <- jokeService.joke

      (timestamp, subject) <- verificationService.generateSubject

      response <- emailService.send {
        Email(destination, sender, subject, Some(VerificationEmail(timestamp, joke).body))
      }
    } yield response
}
