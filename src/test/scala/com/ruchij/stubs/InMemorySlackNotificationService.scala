package com.ruchij.stubs

import java.util.concurrent.ConcurrentHashMap

import cats.effect.Sync
import cats.implicits._
import com.ruchij.services.slack.SlackNotificationService
import com.ruchij.services.slack.models.SlackChannel

import scala.collection.mutable

class InMemorySlackNotificationService[F[_]: Sync](val channels: ConcurrentHashMap[SlackChannel, mutable.Queue[String]])
    extends SlackNotificationService[F] {
  override type Response = ConcurrentHashMap[SlackChannel, mutable.Queue[String]]

  override def notify(slackChannel: SlackChannel, message: String): F[ConcurrentHashMap[SlackChannel, mutable.Queue[String]]] =
    Sync[F].delay {
      Option(channels.get(slackChannel))
        .fold(channels.put(slackChannel, mutable.Queue(message))) {
          _.enqueue(message)
        }
    }
      .as(channels)
}

object InMemorySlackNotificationService {
  def apply[F[_]: Sync]: InMemorySlackNotificationService[F] =
    new InMemorySlackNotificationService[F](new ConcurrentHashMap())
}
