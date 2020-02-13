package com.ruchij.services.slack.models

import cats.effect.Sync
import org.http4s.EntityDecoder

sealed trait SlackResponse

object SlackResponse {
  case object OK extends SlackResponse
  case object Failure extends SlackResponse

  def parse(input: String): SlackResponse =
    if (input.equalsIgnoreCase(OK.toString)) OK else Failure

  implicit def slackResponseEntityDecoder[F[_]: Sync]: EntityDecoder[F, SlackResponse] =
    EntityDecoder.text.map(parse)
}
