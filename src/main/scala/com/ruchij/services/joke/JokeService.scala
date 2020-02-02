package com.ruchij.services.joke

import cats.effect.Sync
import com.github.javafaker.Faker

class JokeService[F[_]: Sync](faker: Faker) {

  val joke: F[String] = Sync[F].delay(faker.chuckNorris().fact())
}
