package com.ruchij.lambda

import cats.effect.{Blocker, Clock, ContextShift, Sync}
import cats.implicits._
import cats.{Monad, ~>}
import com.github.javafaker.Faker
import com.ruchij.config.{SendGridConfiguration, VerificationConfiguration}
import com.ruchij.services.email.models.Email
import com.ruchij.services.email.models.Email.EmailAddress
import com.ruchij.services.email.{EmailService, SendGridEmailService}
import com.ruchij.services.joke.JokeService
import com.ruchij.services.verify.VerificationService
import com.sendgrid.SendGrid
import html.VerificationEmail
import pureconfig.ConfigObjectSource

object SendGridHandler {

  def create[F[_]: Sync: ContextShift: Clock: Either[Throwable, *] ~> *[_]](
    configObjectSource: ConfigObjectSource,
    blocker: Blocker
  ): F[EmailService[F]#Response] =
    for {
      sendGridConfiguration <- SendGridConfiguration.load[F](configObjectSource)
      verificationConfiguration <- VerificationConfiguration.load[F](configObjectSource)

      sendGridEmailService = new SendGridEmailService[F](new SendGrid(sendGridConfiguration.apiKey), blocker)
      verificationService = new VerificationService[F](verificationConfiguration.messagePeriod)
      jokeService = new JokeService[F](Faker.instance())

      result <- run(sendGridEmailService, verificationService, jokeService)(
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
        Email(destination, sender, subject, VerificationEmail(timestamp, joke).body)
      }
    } yield response
}
