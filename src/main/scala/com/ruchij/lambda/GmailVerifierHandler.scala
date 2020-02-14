package com.ruchij.lambda

import cats.effect.{Blocker, Clock, ContextShift, IO, Sync, Timer}
import cats.implicits._
import cats.~>
import com.amazonaws.services.lambda.runtime.events.ScheduledEvent
import com.amazonaws.services.lambda.runtime.{Context, RequestHandler}
import com.ruchij.config.{GmailConfiguration, SendGridConfiguration, SlackConfiguration, VerificationConfiguration}
import com.ruchij.services.email.SendGridEmailService
import com.ruchij.services.gmail.GmailServiceImpl
import com.ruchij.services.gmail.models.GmailMessage
import com.ruchij.services.slack.SlackNotificationServiceImpl
import com.ruchij.services.verify.VerificationService
import com.ruchij.types.FunctionKTypes.fromThrowableEither
import com.sendgrid.SendGrid
import org.http4s.client.{Client, JavaNetClientBuilder}
import pureconfig.{ConfigObjectSource, ConfigSource}

import scala.concurrent.ExecutionContext

class GmailVerifierHandler extends RequestHandler[ScheduledEvent, Unit] {

  override def handleRequest(scheduledEvent: ScheduledEvent, context: Context): Unit = {
    implicit val contextShift: ContextShift[IO] = IO.contextShift(ExecutionContext.global)
    implicit val timer: Timer[IO] = IO.timer(ExecutionContext.global)

    val blocker: Blocker = Blocker.liftExecutionContext(ExecutionContext.global)
    val configObjectSource: ConfigObjectSource = ConfigSource.defaultApplication
    val client = JavaNetClientBuilder[IO](blocker).create

    GmailVerifierHandler.create[IO](configObjectSource, blocker, client).unsafeRunSync()
  }
}

object GmailVerifierHandler {

  def create[F[_]: Sync: Clock: ContextShift: Either[Throwable, *] ~> *[_]](
    configObjectSource: ConfigObjectSource,
    blocker: Blocker,
    httpClient: Client[F]
  ): F[GmailMessage] =
    for {
      VerificationConfiguration(messagePeriod, _, adminEmails, sender) <- VerificationConfiguration.load[F](
        configObjectSource
      )

      gmailConfiguration <- GmailConfiguration.load[F](configObjectSource)
      sendGridConfiguration <- SendGridConfiguration.load[F](configObjectSource)
      slackConfiguration <- SlackConfiguration.load[F](configObjectSource)

      gmailService <- GmailServiceImpl.create[F](gmailConfiguration, blocker)
      sendGridEmailService = new SendGridEmailService[F](new SendGrid(sendGridConfiguration.apiKey), blocker)
      slackNotificationService = new SlackNotificationServiceImpl[F](httpClient, slackConfiguration)

      result <- VerificationService
        .verify(sender, messagePeriod, adminEmails)
        .run((gmailService, sendGridEmailService, slackNotificationService))
    } yield result
}
