package com.ruchij.utils

import cats.{Applicative, Functor, MonadError}

object MonadicUtils {
  def anyOf[A, B, F[_]: MonadError[*[_], B], M[x] <: Iterable[x]](values: M[F[A]]): F[Option[A]]  =
    values.headOption.fold[F[Option[A]]](Applicative[F].pure(None)) {
      head =>
        MonadError[F, B].handleErrorWith(Functor[F].map(head) { value => Option(value) }) {
          _ => anyOf(values.tail)
        }
    }
}
