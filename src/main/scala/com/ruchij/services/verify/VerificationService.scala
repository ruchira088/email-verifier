package com.ruchij.services.verify

import java.util.concurrent.TimeUnit

import cats.{Applicative, MonadError}
import cats.effect.{Clock, Sync}
import cats.implicits._
import com.ruchij.config.VerificationConfiguration
import com.ruchij.services.email.EmailService
import com.ruchij.services.email.models.Email
import com.ruchij.services.gmail.GmailService
import com.ruchij.services.gmail.models.GmailMessage
import com.ruchij.services.joke.JokeService
import com.ruchij.services.verify.exception.VerificationFailedException
import com.ruchij.utils.MonadicUtils
import html.{FailureNotificationEmail, VerificationEmail}
import org.joda.time.DateTime

class VerificationService[F[_]: Clock: Sync](
  emailService: EmailService[F],
  jokeService: JokeService[F],
  gmailService: GmailService[F],
  verificationConfiguration: VerificationConfiguration
) {
  val SUBJECT_PREFIX = "Verification Email sent at "

  val sendVerificationEmail: F[EmailService[F]#Response] =
    for {
      timestamp <- Clock[F].realTime(TimeUnit.MILLISECONDS)
      joke <- jokeService.joke

      response <- emailService.send {
        Email(
          verificationConfiguration.primaryEmail,
          verificationConfiguration.sender,
          SUBJECT_PREFIX + new DateTime(timestamp),
          Some(VerificationEmail(new DateTime(timestamp), joke).body)
        )
      }
    } yield response

  val verify: F[GmailMessage] =
    for {
      emailMessages <- gmailService.fetchMessages(verificationConfiguration.sender, None)

      result <- MonadicUtils.anyOf {
        emailMessages.gmailMessages.map { gmailMessage =>
          verifyEmail(gmailMessage.email).as(gmailMessage)
        }
      }

      timestamp <- Clock[F].realTime(TimeUnit.MILLISECONDS)

      successfulMessage <- result.fold[F[GmailMessage]](
        notifyFailure(new DateTime(timestamp))
          .productR {
            MonadError[F, Throwable]
              .raiseError(VerificationFailedException("Unable to find any messages to satisfy verification"))
          }
      )(Applicative[F].pure)

      _ <- gmailService.deleteMessage(successfulMessage.messageId)
    } yield successfulMessage

  def verifyEmail(email: Email): F[DateTime] =
    if (email.subject.startsWith(SUBJECT_PREFIX))
      for {
        sentDateTime <- Sync[F].delay { DateTime.parse(email.subject.substring(SUBJECT_PREFIX.length).trim) }

        currentTimestamp <- Clock[F].realTime(TimeUnit.MILLISECONDS)

        earliestAcceptedTimestamp = new DateTime(currentTimestamp)
          .minus(verificationConfiguration.messagePeriod.toMillis)

        _ <- if (earliestAcceptedTimestamp.isBefore(sentDateTime))
          Applicative[F].unit
        else
          Sync[F].raiseError[Unit] {
            VerificationFailedException(s"Unable to find an email sent after $earliestAcceptedTimestamp")
          }
      } yield sentDateTime
    else
      Sync[F].raiseError {
        VerificationFailedException(s"Email subject does NOT start with $SUBJECT_PREFIX")
      }

  def notifyFailure(dateTime: DateTime): F[List[EmailService[F]#Response]] =
    verificationConfiguration.adminEmails.traverse { emailAddress =>
      emailService
        .send {
          Email(
            emailAddress,
            verificationConfiguration.sender,
            s"Email verification failed at $dateTime",
            Some(FailureNotificationEmail(dateTime).body)
          )
        }
        .map(identity[EmailService[F]#Response])
    }
}
