package com.ruchij.services.verify

import java.util.concurrent.TimeUnit

import cats.data.ReaderT
import cats.effect.{Clock, Sync}
import cats.implicits._
import cats.{Applicative, Monad, MonadError}
import com.ruchij.services.email.EmailService
import com.ruchij.services.email.models.Email
import com.ruchij.services.email.models.Email.EmailAddress
import com.ruchij.services.gmail.GmailService
import com.ruchij.services.gmail.models.GmailMessage
import com.ruchij.services.joke.JokeService
import com.ruchij.services.verify.exception.VerificationFailedException
import com.ruchij.utils.MonadicUtils
import html.{FailureNotificationEmail, VerificationEmail}
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat

import scala.concurrent.duration.FiniteDuration

object VerificationService {
  val SUBJECT_PREFIX = "Verification Email sent at "

  def sendVerificationEmail[F[_]: Clock: Monad](
    to: EmailAddress,
    from: EmailAddress
  ): ReaderT[F, (JokeService[F], EmailService[F]), EmailService[F]#Response] =
    ReaderT {
      case (jokeService, emailService) =>
        for {
          timestamp <- Clock[F].realTime(TimeUnit.MILLISECONDS)
          joke <- jokeService.joke

          response <- emailService.send {
            Email(
              to,
              from,
              SUBJECT_PREFIX + new DateTime(timestamp).toString(DateTimeFormat.mediumDateTime()),
              Some(VerificationEmail(new DateTime(timestamp), joke).body)
            )
          }
        } yield response
    }

  def verify[F[_]: Clock: Sync](
    sender: EmailAddress,
    period: FiniteDuration,
    adminEmails: List[EmailAddress]
  ): ReaderT[F, (GmailService[F], EmailService[F]), GmailMessage] =
    ReaderT {
      case (gmailService, emailService) =>
        for {
          emailMessages <- gmailService.fetchMessages(sender, None)

          result <- MonadicUtils.anyOf {
            emailMessages.gmailMessages.map { gmailMessage =>
              verifyEmail(gmailMessage.email, period).as(gmailMessage)
            }
          }

          timestamp <- Clock[F].realTime(TimeUnit.MILLISECONDS)

          successfulMessage <- result.fold[F[GmailMessage]](
            notifyFailure[F](new DateTime(timestamp), sender, adminEmails)
              .run(emailService)
              .productR {
                MonadError[F, Throwable]
                  .raiseError(VerificationFailedException("Unable to find any messages to satisfy verification"))
              }
          )(Applicative[F].pure)

          _ <- gmailService.deleteMessage(successfulMessage.messageId)
        } yield successfulMessage
    }

  def verifyEmail[F[_]: Sync: Clock](email: Email, period: FiniteDuration): F[DateTime] =
    if (email.subject.startsWith(SUBJECT_PREFIX))
      for {
        sentDateTime <- Sync[F].delay { DateTime.parse(email.subject.substring(SUBJECT_PREFIX.length).trim, DateTimeFormat.mediumDateTime()) }

        currentTimestamp <- Clock[F].realTime(TimeUnit.MILLISECONDS)

        earliestAcceptedTimestamp = new DateTime(currentTimestamp).minus(period.toMillis)

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

  def notifyFailure[F[_]: Applicative](
    dateTime: DateTime,
    sender: EmailAddress,
    adminEmails: List[EmailAddress]
  ): ReaderT[F, EmailService[F], List[EmailService[F]#Response]] =
    ReaderT { emailService =>
      adminEmails.traverse { emailAddress =>
        emailService
          .send {
            Email(
              emailAddress,
              sender,
              s"Email verification failed at $dateTime",
              Some(FailureNotificationEmail(dateTime).body)
            )
          }
          .map(identity[EmailService[F]#Response])
      }
    }
}
