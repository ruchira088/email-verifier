package com.ruchij.services.email

import cats.effect.{Blocker, ContextShift, Sync}
import com.ruchij.services.email.models.Email
import com.sendgrid.helpers.mail.Mail
import com.sendgrid.helpers.mail.objects.{Content, Email => SendGridEmail}
import com.sendgrid.{Method, Request, Response => SendGridResponse, SendGrid}
import org.apache.http.entity.ContentType

class SendGridEmailService[F[_]: Sync: ContextShift](sendGrid: SendGrid, blocker: Blocker) extends EmailService[F] {
  override type Response = SendGridResponse

  override def send(email: Email): F[SendGridResponse] =
    blocker.delay {
      sendGrid.api {
        new Request {
          setMethod(Method.POST)
          setEndpoint("mail/send")
          setBody {
            new Mail(
              new SendGridEmail(email.from),
              email.subject,
              new SendGridEmail(email.to),
              email.body.fold(new Content()) { body =>
                new Content(ContentType.TEXT_HTML.getMimeType, body)
              }
            ).build()
          }
        }
      }
    }
}
