package com.ruchij.lambda

import cats.effect.{Blocker, Clock, ContextShift, Sync}
import cats.implicits._
import cats.~>
import com.ruchij.config.{GmailConfiguration, SendGridConfiguration, VerificationConfiguration}
import com.ruchij.services.email.{EmailService, SendGridEmailService}
import com.ruchij.services.gmail.GmailServiceImpl
import com.ruchij.services.joke.ChuckNorrisJokeService
import com.ruchij.services.verify.VerificationService
import com.sendgrid.SendGrid
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
      gmailConfiguration <- GmailConfiguration.load[F](configObjectSource)

      sendGridEmailService = new SendGridEmailService[F](new SendGrid(sendGridConfiguration.apiKey), blocker)
      gmailService <- GmailServiceImpl.create(gmailConfiguration, blocker)
      chuckNorrisJokeService = new ChuckNorrisJokeService[F](client)

      verificationService = new VerificationService[F](
        sendGridEmailService,
        chuckNorrisJokeService,
        gmailService,
        verificationConfiguration
      )

      result <- verificationService.sendVerificationEmail
    } yield result
}
