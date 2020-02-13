package com.ruchij.lambda

import cats.effect.{Blocker, Clock, ContextShift, Sync}
import cats.implicits._
import cats.~>
import com.ruchij.config.{GmailConfiguration, SendGridConfiguration, SlackConfiguration, VerificationConfiguration}
import com.ruchij.services.email.SendGridEmailService
import com.ruchij.services.gmail.GmailServiceImpl
import com.ruchij.services.gmail.models.GmailMessage
import com.ruchij.services.slack.SlackNotificationServiceImpl
import com.ruchij.services.verify.VerificationService
import com.sendgrid.SendGrid
import org.http4s.client.Client
import pureconfig.ConfigObjectSource

object GmailVerifierHandler {

  def create[F[_]: Sync: Clock: ContextShift: Either[Throwable, *] ~> *[_]](
    configObjectSource: ConfigObjectSource,
    blocker: Blocker,
    httpClient: Client[F]
  ): F[GmailMessage] =
    for {
      VerificationConfiguration(messagePeriod, _, adminEmails, sender) <-
        VerificationConfiguration.load[F](configObjectSource)

      gmailConfiguration <- GmailConfiguration.load[F](configObjectSource)
      sendGridConfiguration <- SendGridConfiguration.load[F](configObjectSource)
      slackConfiguration <- SlackConfiguration.load[F](configObjectSource)

      gmailService <- GmailServiceImpl.create[F](gmailConfiguration, blocker)
      sendGridEmailService = new SendGridEmailService[F](new SendGrid(sendGridConfiguration.apiKey), blocker)
      slackNotificationService = new SlackNotificationServiceImpl[F](httpClient, slackConfiguration)

      result <-
        VerificationService.verify(sender, messagePeriod, adminEmails)
        .run((gmailService, sendGridEmailService, slackNotificationService))
    } yield result
}
