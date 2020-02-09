package com.ruchij.services.verify

import java.util.concurrent.ConcurrentHashMap

import cats.effect.{Clock, IO}
import com.github.javafaker.Faker
import com.ruchij.services.email.models.Email
import com.ruchij.services.gmail.models.GmailMessage
import com.ruchij.services.joke.LocalJokeService
import com.ruchij.services.verify.exception.VerificationFailedException
import com.ruchij.stubs.{InMemoryEmailService, InMemoryGmailService}
import com.ruchij.test.Providers
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers
import org.scalatest.{EitherValues, OptionValues}

import scala.collection.mutable
import scala.concurrent.duration._
import scala.language.postfixOps

class VerificationServiceTest extends AnyFlatSpec with Matchers with EitherValues with OptionValues {

  "sendVerificationEmail" should "send a verification email to the email service" in {
    val dateTime = DateTime.now().withMillisOfSecond(0)
    implicit val clock: Clock[IO] = Providers.stubClock(dateTime)

    val jokeService = new LocalJokeService[IO](Faker.instance())
    val emailService = new InMemoryEmailService[IO](mutable.Queue.empty)

    val to = Email.lift("to@ruchij.com")
    val from = Email.lift("from@ruchij.com")

    VerificationService.sendVerificationEmail[IO](to, from).run((jokeService, emailService)).unsafeRunSync()

    emailService.emails must not be empty

    val sentEmail = emailService.emails.headOption.value

    sentEmail.to mustBe to
    sentEmail.from mustBe from
    sentEmail.subject must startWith(VerificationService.SUBJECT_PREFIX)
    sentEmail.subject must endWith(dateTime.toString(DateTimeFormat.mediumDateTime()))
    sentEmail.body must not be None
  }

  "verify" should "return the verified gmail message when successfully verified" in {
    val dateTime = DateTime.now()
    implicit val clock: Clock[IO] = Providers.stubClock(dateTime)

    val gmailMessage = createGmailMessage("gmail-1", dateTime)

    val inMemoryGmailService = new InMemoryGmailService[IO](new ConcurrentHashMap())
    inMemoryGmailService.putMessage(gmailMessage).unsafeRunSync()

    val inMemoryEmailService = new InMemoryEmailService[IO](mutable.Queue.empty)

    VerificationService
      .verify[IO](gmailMessage.email.from, 30 seconds, List.empty)
      .run((inMemoryGmailService, inMemoryEmailService))
      .unsafeRunSync() mustBe gmailMessage
  }

  it should "send messages to admins if the verification failed" in {
    val dateTime = DateTime.now()
    implicit val clock: Clock[IO] = Providers.stubClock(dateTime)

    val adminOne = Email.lift("admin-1@ruchij.com")
    val adminTwo = Email.lift("admin-2@ruchij.com")

    val adminEmails = List(adminOne, adminTwo)
    val gmailMessage = createGmailMessage("gmail-1", dateTime.minusSeconds(60))

    val inMemoryGmailService = new InMemoryGmailService[IO](new ConcurrentHashMap())
    inMemoryGmailService.putMessage(gmailMessage).unsafeRunSync()

    val inMemoryEmailService = new InMemoryEmailService[IO](mutable.Queue.empty)

    VerificationService
      .verify[IO](gmailMessage.email.from, 30 seconds, adminEmails)
      .run((inMemoryGmailService, inMemoryEmailService))
      .attempt
      .unsafeRunSync()
      .left
      .value mustBe a[VerificationFailedException]

    inMemoryEmailService.emails.size mustBe 2

    val sentEmail = inMemoryEmailService.emails.headOption.value

    sentEmail.to mustBe adminOne
    sentEmail.subject.toLowerCase must include("failed")
    sentEmail.subject must include(dateTime.toString(DateTimeFormat.mediumDateTime()))
    sentEmail.body must not be None
  }

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

  it should "return a failure if the email doesn't contain the correct subject prefix" in {
    import Providers.clock

    val email = createEmail(DateTime.now()).copy(subject = "Random Subject")

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

  def createGmailMessage(id: String, dateTime: DateTime): GmailMessage =
    GmailMessage(id, createEmail(dateTime), Map.empty)
}
