package com.ruchij.services.verify

import cats.effect.{Clock, IO}
import com.github.javafaker.Faker
import com.ruchij.services.email.models.Email
import com.ruchij.services.gmail.models.GmailMessage
import com.ruchij.services.joke.LocalJokeService
import com.ruchij.services.verify.exception.VerificationFailedException
import com.ruchij.stubs.{InMemoryEmailService, InMemoryGmailService, InMemorySlackNotificationService}
import com.ruchij.test.Providers
import org.joda.time.{DateTime, DateTimeZone}
import org.joda.time.format.DateTimeFormat
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers
import org.scalatest.{EitherValues, OptionValues}

import scala.concurrent.duration._
import scala.jdk.CollectionConverters._
import scala.language.postfixOps

class VerificationServiceTest extends AnyFlatSpec with Matchers with EitherValues with OptionValues {

  val dateTimeZone: DateTimeZone =
    Providers.randomPick[IO, String](DateTimeZone.getAvailableIDs.asScala.toSeq)
      .flatMap(id => IO.delay(DateTimeZone.forID(id))).unsafeRunSync()

  "sendVerificationEmail" should "send a verification email to the email service" in {
    val dateTime = DateTime.now().withMillisOfSecond(0)
    implicit val clock: Clock[IO] = Providers.stubClock(dateTime)

    val jokeService = new LocalJokeService[IO](Faker.instance())
    val emailService = InMemoryEmailService[IO]
    val slackNotificationService = InMemorySlackNotificationService[IO]

    val to = Email.lift("to@ruchij.com")
    val from = Email.lift("from@ruchij.com")

    VerificationService.sendVerificationEmail[IO](to, from, dateTimeZone).run((jokeService, emailService, slackNotificationService)).unsafeRunSync()

    emailService.emails must not be empty

    val sentEmail = emailService.emails.headOption.value

    sentEmail.to mustBe to
    sentEmail.from mustBe from
    sentEmail.subject must startWith(VerificationService.SUBJECT_PREFIX)
    sentEmail.subject must endWith(dateTime.toString(DateTimeFormat.mediumDateTime().withZone(dateTimeZone)))
    sentEmail.body must not be None
  }

  "verify" should "return the verified gmail message when successfully verified" in {
    val dateTime = DateTime.now()
    implicit val clock: Clock[IO] = Providers.stubClock(dateTime)

    val gmailMessage = createGmailMessage("gmail-1", dateTime, dateTimeZone)

    val inMemoryGmailService = InMemoryGmailService[IO]
    inMemoryGmailService.putMessage(gmailMessage).unsafeRunSync()

    val inMemoryEmailService = InMemoryEmailService[IO]
    val slackNotificationService = InMemorySlackNotificationService[IO]

    VerificationService
      .verify[IO](gmailMessage.email.from, 30 seconds, List.empty, dateTimeZone)
      .run((inMemoryGmailService, inMemoryEmailService, slackNotificationService))
      .unsafeRunSync() mustBe gmailMessage
  }

  it should "send messages to admins if the verification failed" in {
    val dateTime = DateTime.now()
    implicit val clock: Clock[IO] = Providers.stubClock(dateTime)

    val adminOne = Email.lift("admin-1@ruchij.com")
    val adminTwo = Email.lift("admin-2@ruchij.com")

    val adminEmails = List(adminOne, adminTwo)
    val gmailMessage = createGmailMessage("gmail-1", dateTime.minusSeconds(60), dateTimeZone)

    val inMemoryGmailService = InMemoryGmailService[IO]
    inMemoryGmailService.putMessage(gmailMessage).unsafeRunSync()

    val inMemoryEmailService = InMemoryEmailService[IO]
    val slackNotificationService = InMemorySlackNotificationService[IO]

    VerificationService
      .verify[IO](gmailMessage.email.from, 30 seconds, adminEmails, dateTimeZone)
      .run((inMemoryGmailService, inMemoryEmailService, slackNotificationService))
      .attempt
      .unsafeRunSync()
      .left
      .value mustBe a[VerificationFailedException]

    inMemoryEmailService.emails.size mustBe 2

    val sentEmail = inMemoryEmailService.emails.headOption.value

    sentEmail.to mustBe adminOne
    sentEmail.subject.toLowerCase must include("failed")
    sentEmail.subject must include(dateTime.toString(DateTimeFormat.mediumDateTime().withZone(dateTimeZone)))
    sentEmail.body must not be None
  }

  "verifyEmail" should "return email sent timestamp when successfully verified" in {
    val dateTime = DateTime.now().withMillisOfSecond(0)
    val email = createEmail(dateTime, dateTimeZone)

    implicit val clock: Clock[IO] = Providers.stubClock[IO](dateTime)

    VerificationService.verifyEmail[IO](email, 30 seconds, dateTimeZone).unsafeRunSync() mustBe dateTime.withZone(dateTimeZone)
  }

  it should "return a failure if email sent time is expired" in {
    val dateTime = DateTime.now()
    val email = createEmail(dateTime, dateTimeZone)

    implicit val clock: Clock[IO] = Providers.stubClock[IO](dateTime.plusSeconds(31))

    VerificationService
      .verifyEmail[IO](email, 30 seconds, dateTimeZone)
      .attempt
      .unsafeRunSync()
      .left
      .value mustBe a[VerificationFailedException]
  }

  it should "return a failure if the email doesn't contain the correct subject prefix" in {
    import Providers.clock

    val email = createEmail(DateTime.now(), dateTimeZone).copy(subject = "Random Subject")

    VerificationService
      .verifyEmail[IO](email, 30 seconds, dateTimeZone)
      .attempt
      .unsafeRunSync()
      .left
      .value mustBe a[VerificationFailedException]
  }

  def createEmail(dateTime: DateTime, timeZone: DateTimeZone): Email =
    Email(
      Email.lift("to@ruchij.com"),
      Email.lift("from@ruchij.com"),
      VerificationService.SUBJECT_PREFIX + dateTime.toString(DateTimeFormat.mediumDateTime().withZone(timeZone)),
      Some("This is a test email")
    )

  def createGmailMessage(id: String, dateTime: DateTime, timeZone: DateTimeZone): GmailMessage =
    GmailMessage(id, createEmail(dateTime, timeZone), Map.empty)
}
