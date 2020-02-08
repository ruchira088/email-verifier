package com.ruchij.utils

import cats.effect.{Bracket, Effect, IO, Sync, Timer}
import cats.implicits._
import cats.{Applicative, Functor, MonadError}

import scala.concurrent.duration.FiniteDuration

object MonadicUtils {
  def anyOf[A, B, F[_]: MonadError[*[_], B], M[x] <: Iterable[x]](values: M[F[A]]): F[Option[A]] =
    values.headOption.fold[F[Option[A]]](Applicative[F].pure(None)) { head =>
      MonadError[F, B].handleErrorWith(Functor[F].map(head) { value =>
        Option(value)
      }) { _ =>
        anyOf(values.tail)
      }
    }

  def retryWithDelay[F[_]: Sync: Timer, A](value: F[A], delay: FiniteDuration, retryCount: Int): F[A] =
    Bracket[F, Throwable].handleErrorWith(value) { throwable =>
      if (retryCount == 0)
        Bracket[F, Throwable].raiseError(throwable)
      else
        Sync[F]
          .delay(println(s"Failed. $retryCount retries remaining. Trying again in $delay"))
          .productR(Timer[F].sleep(delay))
          .productR(retryWithDelay(value, delay, retryCount - 1))
    }
}
