package com.ruchij.services.joke

import cats.effect.Sync
import cats.implicits._
import com.github.javafaker.Faker

import scala.util.Random

class JokeService[F[_]: Sync](faker: Faker) {

  val joke: F[String] =
    random(
      Sync[F].delay(faker.chuckNorris().fact()),
      Sync[F].delay(faker.rickAndMorty().quote()),
      Sync[F].delay(faker.howIMetYourMother().quote())
    )
      .map(_.replaceAll("’", "'").replaceAll("…", "."))

  def random[A](values: F[A]*): F[A] =
    if (values.nonEmpty)
      Sync[F].delay(Random.nextInt(values.length))
        .flatMap(values.apply)
    else
      Sync[F].raiseError(new IllegalArgumentException("values CANNOT be empty"))

}
