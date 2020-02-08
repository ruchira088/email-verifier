package com.ruchij.services.joke.models

import cats.effect.Sync
import org.http4s.EntityDecoder
import org.http4s.circe.jsonOf
import io.circe.generic.auto._

case class ChuckNorrisJoke(value: String)

object ChuckNorrisJoke {
  implicit def chuckNorrisJokeCirceDecoder[F[_]: Sync]: EntityDecoder[F, ChuckNorrisJoke] = jsonOf[F, ChuckNorrisJoke]
}
