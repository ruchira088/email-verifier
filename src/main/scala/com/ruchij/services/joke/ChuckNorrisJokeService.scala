package com.ruchij.services.joke

import cats.effect.Sync
import org.http4s.client.Client
import org.http4s.headers.{AgentProduct, `User-Agent`}
import org.http4s.implicits._
import org.http4s.{Headers, Request}

class ChuckNorrisJokeService[F[_]: Sync](httpClient: Client[F]) extends JokeService[F] {

  override val joke: F[String] =
    httpClient.fetchAs[String] {
      Request[F](
        uri = uri"https://api.chucknorris.io/jokes/random",
        // api.chucknorris.io returns an error response when user-agent is "Java/1.8.0_232"
        headers = Headers.of(`User-Agent`(AgentProduct("email-verifier")))
      )
    }
}
