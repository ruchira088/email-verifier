package com.ruchij.types

import cats.effect.IO
import cats.~>

object FunctionKTypes {

  implicit val fromThrowableEither: Either[Throwable, *] ~> IO =
    new ~>[Either[Throwable, *], IO] {
      override def apply[A](either: Either[Throwable, A]): IO[A] =
        IO.fromEither(either)
    }
}
