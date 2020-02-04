package com.ruchij.services.verify

import java.util.concurrent.TimeUnit

import cats.Applicative
import cats.effect.{Clock, Sync}
import cats.implicits._
import com.ruchij.services.email.models.Email
import com.ruchij.services.verify.exception.VerificationFailedException
import org.joda.time.DateTime

import scala.concurrent.duration.FiniteDuration

class VerificationService[F[_]: Clock: Sync](messagePeriod: FiniteDuration) {
  val SUBJECT_PREFIX = "Verification Email sent at "

  val generateSubject: F[(DateTime, String)] =
    Clock[F].realTime(TimeUnit.MILLISECONDS)
      .map { timestamp =>
        new DateTime(timestamp) -> (SUBJECT_PREFIX + new DateTime(timestamp))
      }

  def verify(email: Email): F[DateTime] =
    if (email.subject.startsWith(SUBJECT_PREFIX))
      for {
        sentDateTime <- Sync[F].delay { DateTime.parse(email.subject.substring(SUBJECT_PREFIX.length).trim) }

        currentTimestamp <- Clock[F].realTime(TimeUnit.MILLISECONDS)

        earliestAcceptedTimestamp = new DateTime(currentTimestamp).minus(messagePeriod.toMillis)

        _ <-
          if (earliestAcceptedTimestamp.isBefore(sentDateTime))
            Applicative[F].unit
          else
            Sync[F].raiseError[Unit] {
              VerificationFailedException(s"Unable to find an email sent after $earliestAcceptedTimestamp")
            }
      }
      yield sentDateTime

    else
      Sync[F].raiseError {
        VerificationFailedException(s"Email subject does NOT start with $SUBJECT_PREFIX")
      }
}
