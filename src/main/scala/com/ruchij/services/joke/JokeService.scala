package com.ruchij.services.joke

trait JokeService[F[_]] {
  val joke: F[String]
}
