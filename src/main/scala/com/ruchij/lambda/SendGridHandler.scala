package com.ruchij.lambda

import cats.effect.{Blocker, Clock, ContextShift, IO, Sync, Timer}
import cats.implicits._
import cats.~>
import com.amazonaws.services.lambda.runtime.events.ScheduledEvent
import com.amazonaws.services.lambda.runtime.{Context, RequestHandler}
import com.ruchij.config.{SendGridConfiguration, SlackConfiguration, VerificationConfiguration}
import com.ruchij.services.email.{EmailService, SendGridEmailService}
import com.ruchij.services.joke.ChuckNorrisJokeService
import com.ruchij.services.slack.SlackNotificationServiceImpl
import com.ruchij.services.verify.VerificationService
import com.ruchij.types.FunctionKTypes.fromThrowableEither
import com.sendgrid.SendGrid
import org.http4s.client.{Client, JavaNetClientBuilder}
import pureconfig.{ConfigObjectSource, ConfigSource}

import scala.concurrent.ExecutionContext

class SendGridHandler extends RequestHandler[ScheduledEvent, Unit] {

  override def handleRequest(scheduledEvent: ScheduledEvent, context: Context): Unit = {
    implicit val contextShift: ContextShift[IO] = IO.contextShift(ExecutionContext.global)
    implicit val timer: Timer[IO] = IO.timer(ExecutionContext.global)

    val blocker = Blocker.liftExecutionContext(ExecutionContext.global)
    val configObjectSource = ConfigSource.defaultApplication
    val client = JavaNetClientBuilder[IO](blocker).create

    SendGridHandler
      .create[IO](configObjectSource, blocker, client)
      .unsafeRunSync()
  }
}

object SendGridHandler {

  def create[F[_]: Sync: ContextShift: Clock: Either[Throwable, *] ~> *[_]](
    configObjectSource: ConfigObjectSource,
    blocker: Blocker,
    client: Client[F]
  ): F[EmailService[F]#Response] =
    for {
      sendGridConfiguration <- SendGridConfiguration.load[F](configObjectSource)
      verificationConfiguration <- VerificationConfiguration.load[F](configObjectSource)
      slackConfiguration <- SlackConfiguration.load[F](configObjectSource)

      sendGridEmailService = new SendGridEmailService[F](new SendGrid(sendGridConfiguration.apiKey), blocker)
      chuckNorrisJokeService = new ChuckNorrisJokeService[F](client)
      slackNotificationService = new SlackNotificationServiceImpl[F](client, slackConfiguration)

      result <- VerificationService
        .sendVerificationEmail(verificationConfiguration.primaryEmail, verificationConfiguration.sender)
        .run((chuckNorrisJokeService, sendGridEmailService, slackNotificationService))
    } yield result
}
