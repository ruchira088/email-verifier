package com.ruchij.services.verify

import cats.effect.{Clock, IO}
import com.ruchij.services.email.models.Email
import com.ruchij.services.verify.exception.VerificationFailedException
import com.ruchij.test.Providers
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import org.scalatest.EitherValues
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers

import scala.concurrent.duration._
import scala.language.postfixOps

class VerificationServiceTest extends AnyFlatSpec with Matchers with EitherValues {
  "verifyEmail" should "return email sent timestamp when successfully verified" in {
    val dateTime = DateTime.now().withMillisOfSecond(0)
    val email = createEmail(dateTime)

    implicit val clock: Clock[IO] = Providers.stubClock[IO](dateTime)

    VerificationService.verifyEmail[IO](email, 30 seconds).unsafeRunSync() mustBe dateTime
  }

  it should "return a failure if email sent time is expired" in {
    val dateTime = DateTime.now()
    val email = createEmail(dateTime)

    implicit val clock: Clock[IO] = Providers.stubClock[IO](dateTime.plusSeconds(31))

    VerificationService
      .verifyEmail[IO](email, 30 seconds)
      .attempt
      .unsafeRunSync()
      .left
      .value mustBe a[VerificationFailedException]
  }

  def createEmail(dateTime: DateTime): Email =
    Email(
      Email.lift("to@ruchij.com"),
      Email.lift("from@ruchij.com"),
      VerificationService.SUBJECT_PREFIX + dateTime.toString(DateTimeFormat.mediumDateTime()),
      Some("This is a test email")
    )
}
