package com.ruchij.services.gmail

import com.ruchij.services.email.models.Email.EmailAddress
import com.ruchij.services.gmail.models.FetchedEmails

trait GmailService[F[_]] {
  def fetchMessages(from: EmailAddress, pageToken: Option[String]): F[FetchedEmails]
}
