package com.ruchij.services.slack

import com.ruchij.services.slack.models.SlackChannel

trait SlackNotificationService[F[_]] {
  type Response

  def notify(slackChannel: SlackChannel, message: String): F[Response]
}