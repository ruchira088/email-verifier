package com.ruchij.services.email.models

import com.ruchij.services.email.models.Email.EmailAddress
import shapeless.tag.@@
import shapeless.tag

case class Email(to: EmailAddress, from: EmailAddress, subject: String, body: String)

object Email {
  trait EmailAddressTag

  type EmailAddress = String @@ EmailAddressTag

  def lift(emailAddress: String): EmailAddress = tag[EmailAddressTag][String](emailAddress)
}