package com.ruchij.services.slack.models

import org.http4s.EntityEncoder
import org.http4s.circe.jsonEncoderOf
import io.circe.generic.auto._

case class SlackMessageBody(text: String)

object SlackMessageBody {
  implicit def slackMessageBodyEntityEncoder[F[_]]: EntityEncoder[F, SlackMessageBody] =
    jsonEncoderOf[F, SlackMessageBody]
}
