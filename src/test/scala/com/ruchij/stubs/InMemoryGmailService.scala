package com.ruchij.stubs

import java.util.concurrent.{ConcurrentHashMap, ConcurrentMap}

import cats.Applicative
import cats.effect.Sync
import com.ruchij.services.email.models.Email.EmailAddress
import com.ruchij.services.gmail.GmailService
import com.ruchij.services.gmail.models.{GmailMessage, GmailMessagesResult}

import scala.jdk.CollectionConverters._

class InMemoryGmailService[F[_]: Sync](messages: ConcurrentMap[String, GmailMessage]) extends GmailService[F] {

  override def fetchMessages(from: EmailAddress, pageToken: Option[String]): F[GmailMessagesResult] =
    Applicative[F].pure {
      GmailMessagesResult(
        messages.values().asScala.toList.filter(_.email.from == from),
        None
      )
    }

  override def deleteMessage(messageId: String): F[Unit] =
    Sync[F].delay {
      messages.remove(messageId)
    }

  def putMessage(gmailMessage: GmailMessage): F[Unit] =
    Sync[F].delay {
      messages.put(gmailMessage.messageId, gmailMessage)
    }
}

object InMemoryGmailService {
  def apply[F[_]: Sync]: InMemoryGmailService[F] =
    new InMemoryGmailService[F](new ConcurrentHashMap())
}
