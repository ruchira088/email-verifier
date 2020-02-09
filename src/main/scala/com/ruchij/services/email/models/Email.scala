package com.ruchij.services.email.models

import com.ruchij.services.email.models.Email.EmailAddress
import shapeless.tag.@@
import shapeless.tag

case class Email(to: EmailAddress, from: EmailAddress, subject: String, body: Option[String]) {
  override def toString: String =
    s"""
      |From: $from
      |To: $to
      |Subject: $subject
      |${body.fold("") {content => s"Body:\n ${content.split("\n").dropWhile(_.trim.isEmpty).mkString("\n  ")}"}}
      |""".stripMargin
}

object Email {
  trait EmailAddressTag

  type EmailAddress = String @@ EmailAddressTag

  def lift(emailAddress: String): EmailAddress = tag[EmailAddressTag][String](emailAddress)
}
