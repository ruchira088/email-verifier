package com.ruchij.services.joke

import cats.effect.Sync
import cats.implicits._
import com.ruchij.services.joke.models.ChuckNorrisJoke
import org.http4s.Request
import org.http4s.implicits._
import org.http4s.client.Client

class ChuckNorrisJokeService[F[_]: Sync](httpClient: Client[F]) extends JokeService[F] {

  override val joke: F[String] =
    httpClient.expect[ChuckNorrisJoke] {
      Request[F](uri = uri"https://api.chucknorris.io/jokes/random")
    }
      .map(_.value)
}
