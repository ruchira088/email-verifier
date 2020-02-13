package com.ruchij.lambda

import cats.effect.{Blocker, Clock, ContextShift, Sync}
import cats.implicits._
import cats.~>
import com.ruchij.config.{SendGridConfiguration, SlackConfiguration, VerificationConfiguration}
import com.ruchij.services.email.{EmailService, SendGridEmailService}
import com.ruchij.services.joke.ChuckNorrisJokeService
import com.ruchij.services.slack.SlackNotificationServiceImpl
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
      slackConfiguration <- SlackConfiguration.load[F](configObjectSource)

      sendGridEmailService = new SendGridEmailService[F](new SendGrid(sendGridConfiguration.apiKey), blocker)
      chuckNorrisJokeService = new ChuckNorrisJokeService[F](client)
      slackNotificationService = new SlackNotificationServiceImpl[F](client, slackConfiguration)

      result <- VerificationService
        .sendVerificationEmail(verificationConfiguration.primaryEmail, verificationConfiguration.sender)
        .run((chuckNorrisJokeService, sendGridEmailService, slackNotificationService))
    } yield result
}
