package com.ruchij.services.gmail.models

import cats.Applicative
import cats.effect.Bracket
import com.google.api.services.gmail.model.Message
import com.ruchij.services.email.models.Email

import scala.jdk.CollectionConverters.ListHasAsScala

case class GmailMessage(messageId: String, email: Email, headers: Map[String, String])

object GmailMessage {
  def parse[F[_]: Bracket[*[_], Throwable]](message: Message): F[GmailMessage] = {
    val body: Option[String] =
      Option(message.getPayload)
        .flatMap(payload => Option(payload.getBody))
        .flatMap(body => Option(body.decodeData()))
        .map(bytes => new String(bytes))

    val headers: Map[String, String] =
      message.getPayload.getHeaders.asScala.toList.foldLeft(Map.empty[String, String]) {
        case (values, header) => values + (header.getName -> header.getValue)
      }

    val gmailMessageOpt: Option[GmailMessage] =
      for {
        from <- headers.get("From")
        to <- headers.get("To")
        subject <- headers.get("Subject")
      } yield GmailMessage(message.getId, Email(Email.lift(to), Email.lift(from), subject, body), headers)

    gmailMessageOpt.fold[F[GmailMessage]](
      Bracket[F, Throwable].raiseError(new IllegalArgumentException("Unable to extract required email meta-data"))
    )(Applicative[F].pure)
  }
}
