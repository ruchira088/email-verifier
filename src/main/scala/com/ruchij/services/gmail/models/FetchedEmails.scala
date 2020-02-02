package com.ruchij.services.gmail.models

import com.ruchij.services.email.models.Email

case class FetchedEmails(emails: List[Email], nextToken: Option[String])
