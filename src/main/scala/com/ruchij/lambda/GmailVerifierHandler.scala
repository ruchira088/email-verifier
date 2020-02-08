package com.ruchij.lambda

import cats.effect.{Blocker, Clock, ContextShift, Sync}
import cats.implicits._
import cats.{Applicative, MonadError, ~>}
import com.ruchij.config.{GmailConfiguration, VerificationConfiguration}
import com.ruchij.services.email.models.Email.EmailAddress
import com.ruchij.services.gmail.models.GmailMessage
import com.ruchij.services.gmail.{GmailService, GmailServiceImpl}
import com.ruchij.services.verify.VerificationService
import com.ruchij.services.verify.exception.VerificationFailedException
import com.ruchij.utils.MonadicUtils
import pureconfig.ConfigObjectSource

object GmailVerifierHandler {

  def create[F[_]: Sync: Clock: ContextShift: Either[Throwable, *] ~> *[_]](
    configObjectSource: ConfigObjectSource,
    blocker: Blocker
  ): F[GmailMessage] =
    for {
      verificationConfiguration <- VerificationConfiguration.load[F](configObjectSource)
      gmailConfiguration <- GmailConfiguration.load[F](configObjectSource)

      gmailService <- GmailServiceImpl.create[F](gmailConfiguration, blocker)
      verificationService = new VerificationService[F](verificationConfiguration.messagePeriod)

      result <- run(gmailService, verificationService)(gmailConfiguration.sender)
    } yield result

  def run[F[_]: MonadError[*[_], Throwable]](
    gmailService: GmailService[F],
    verificationService: VerificationService[F]
  )(sender: EmailAddress): F[GmailMessage] =
    for {
      emailMessages <- gmailService.fetchMessages(sender, None)

      result <- MonadicUtils.anyOf {
        emailMessages.gmailMessages.map { gmailMessage =>
          verificationService.verify(gmailMessage.email).as(gmailMessage)
        }
      }

      successfulMessage <- result.fold[F[GmailMessage]](
        MonadError[F, Throwable]
          .raiseError(VerificationFailedException("Unable to find any messages to satisfy verification"))
      )(Applicative[F].pure)

      _ <- gmailService.deleteMessage(successfulMessage.messageId)
    } yield successfulMessage
}
