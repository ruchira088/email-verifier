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
import com.ruchij.services.slack.SlackNotificationService
import com.ruchij.services.slack.models.SlackChannel
import com.ruchij.services.verify.exception.VerificationFailedException
import com.ruchij.utils.MonadicUtils
import html.{FailureNotificationEmail, VerificationEmail}
import org.joda.time.{DateTime, DateTimeZone}
import org.joda.time.format.DateTimeFormat

import scala.concurrent.duration.FiniteDuration

object VerificationService {
  val SUBJECT_PREFIX = "Verification Email sent at "

  def sendVerificationEmail[F[_]: Clock: Monad](
    to: EmailAddress,
    from: EmailAddress,
    timeZone: DateTimeZone
  ): ReaderT[F, (JokeService[F], EmailService[F], SlackNotificationService[F]), EmailService[F]#Response] =
    ReaderT {
      case (jokeService, emailService, slackNotificationService) =>
        for {
          timestamp <- Clock[F].realTime(TimeUnit.MILLISECONDS)
          joke <- jokeService.joke

          dateTime = new DateTime(timestamp).withZone(timeZone).toString(DateTimeFormat.mediumDateTime())

          response <- emailService.send {
            Email(
              to,
              from,
              SUBJECT_PREFIX + dateTime,
              Some(VerificationEmail(new DateTime(timestamp).withZone(timeZone), joke).body)
            )
          }

          _ <- slackNotificationService.notify(SlackChannel.EmailVerifier, s"Sent verification email at $dateTime")
        } yield response
    }

  def verify[F[_]: Clock: Sync](
    sender: EmailAddress,
    period: FiniteDuration,
    adminEmails: List[EmailAddress],
    timeZone: DateTimeZone
  ): ReaderT[F, (GmailService[F], EmailService[F], SlackNotificationService[F]), GmailMessage] =
    ReaderT {
      case (gmailService, emailService, slackNotificationService) =>
        for {
          emailMessages <- gmailService.fetchMessages(sender, None)

          result <- MonadicUtils.anyOf {
            emailMessages.gmailMessages.map { gmailMessage =>
              verifyEmail(gmailMessage.email, period, timeZone).as(gmailMessage)
            }
          }

          timestamp <- Clock[F].realTime(TimeUnit.MILLISECONDS)

          successfulMessage <- result.fold[F[GmailMessage]](
            notifyFailure[F](new DateTime(timestamp), sender, adminEmails, timeZone)
              .run((emailService, slackNotificationService))
              .productR {
                MonadError[F, Throwable]
                  .raiseError(VerificationFailedException("Unable to find any messages to satisfy verification"))
              }
          )(Applicative[F].pure)

          _ <- slackNotificationService.notify(
            SlackChannel.EmailVerifier,
            s"Verified email at ${new DateTime(timestamp).withZone(timeZone).toString(DateTimeFormat.mediumDateTime())}"
          )

          _ <- gmailService.deleteMessage(successfulMessage.messageId)
        } yield successfulMessage
    }

  def verifyEmail[F[_]: Sync: Clock](email: Email, period: FiniteDuration, timeZone: DateTimeZone): F[DateTime] =
    if (email.subject.startsWith(SUBJECT_PREFIX))
      for {
        sentDateTime <- Sync[F].delay {
          DateTime.parse(
            email.subject.substring(SUBJECT_PREFIX.length).trim,
            DateTimeFormat.mediumDateTime().withZone(timeZone)
          )
        }

        currentTimestamp <- Clock[F].realTime(TimeUnit.MILLISECONDS)

        earliestAcceptedTimestamp = new DateTime(currentTimestamp).minus(period.toMillis).withZone(timeZone)

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
    adminEmails: List[EmailAddress],
    timeZone: DateTimeZone
  ): ReaderT[
    F,
    (EmailService[F], SlackNotificationService[F]),
    (List[EmailService[F]#Response], SlackNotificationService[F]#Response)
  ] =
    ReaderT {
      case (emailService, slackNotificationService) =>
        val subject = s"Email verification failed at ${dateTime.withZone(timeZone).toString(DateTimeFormat.mediumDateTime())}"

        adminEmails
          .traverse { emailAddress =>
            emailService
              .send {
                Email(emailAddress, sender, subject, Some(FailureNotificationEmail(dateTime).body))
              }
              .map(identity[EmailService[F]#Response])
          }
          .product {
            slackNotificationService
              .notify(SlackChannel.General, subject)
              .map(identity[SlackNotificationService[F]#Response])
          }
    }
}
