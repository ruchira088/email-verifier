package com.ruchij.services.slack

import cats.effect.Sync
import cats.implicits._
import cats.~>
import com.ruchij.config.SlackConfiguration
import com.ruchij.services.slack.SlackNotificationServiceImpl.SLACK_BASE_URL
import com.ruchij.services.slack.models.{SlackChannel, SlackMessageBody, SlackResponse}
import org.http4s.Uri
import org.http4s.client.Client
import org.http4s.client.dsl.Http4sClientDsl
import org.http4s.dsl.Http4sDsl

class SlackNotificationServiceImpl[F[_]: Sync](httpClient: Client[F], slackConfiguration: SlackConfiguration)(
  implicit functionK: Either[Throwable, *] ~> F
) extends SlackNotificationService[F] {
  override type Response = SlackResponse

  val dsl: Http4sDsl[F] = new Http4sDsl[F] {}
  val clientDsl: Http4sClientDsl[F] = new Http4sClientDsl[F] {}

  import clientDsl.http4sWithBodySyntax
  import dsl.POST

  override def notify(slackChannel: SlackChannel, message: String): F[SlackResponse] =
    Sync[F]
      .defer {
        functionK {
          Uri.fromString(SLACK_BASE_URL + slackChannel.secret(slackConfiguration))
        }
      }
      .flatMap { uri =>
        httpClient.expect[SlackResponse] {
          POST(SlackMessageBody(message), uri)
        }
      }
}

object SlackNotificationServiceImpl {
  val SLACK_BASE_URL = "https://hooks.slack.com/services/"
}
