package com.ruchij.services.gmail.models

case class GmailMessagesResult(gmailMessages: List[GmailMessage], nextToken: Option[String])
