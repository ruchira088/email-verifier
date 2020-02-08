package com.ruchij.lambda

import cats.effect.{Blocker, Clock, ContextShift, Sync}
import cats.implicits._
import cats.~>
import com.ruchij.config.{GmailConfiguration, SendGridConfiguration, VerificationConfiguration}
import com.ruchij.services.email.SendGridEmailService
import com.ruchij.services.gmail.GmailServiceImpl
import com.ruchij.services.gmail.models.GmailMessage
import com.ruchij.services.verify.VerificationService
import com.sendgrid.SendGrid
import pureconfig.ConfigObjectSource

object GmailVerifierHandler {

  def create[F[_]: Sync: Clock: ContextShift: Either[Throwable, *] ~> *[_]](
    configObjectSource: ConfigObjectSource,
    blocker: Blocker
  ): F[GmailMessage] =
    for {
      VerificationConfiguration(messagePeriod, _, adminEmails, sender) <-
        VerificationConfiguration.load[F](configObjectSource)

      gmailConfiguration <- GmailConfiguration.load[F](configObjectSource)
      sendGridConfiguration <- SendGridConfiguration.load[F](configObjectSource)

      gmailService <- GmailServiceImpl.create[F](gmailConfiguration, blocker)
      sendGridEmailService = new SendGridEmailService[F](new SendGrid(sendGridConfiguration.apiKey), blocker)

      result <-
        VerificationService.verify(sender, messagePeriod, adminEmails)
        .run((gmailService, sendGridEmailService))
    } yield result
}
