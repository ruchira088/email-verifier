package com.ruchij.services.gmail.models

import com.ruchij.services.email.models.Email

case class GmailMessage(messageId: String, email: Email, headers: Map[String, String])
