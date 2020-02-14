package com.ruchij.utils

import cats.effect.{Bracket, Sync, Timer}
import cats.implicits._
import cats.{Applicative, ApplicativeError, Functor}

import scala.concurrent.duration._
import scala.language.postfixOps

object MonadicUtils {
  def anyOf[A, B, F[_]: ApplicativeError[*[_], B], M[x] <: Iterable[x]](values: M[F[A]]): F[Option[A]] =
    values.headOption.fold[F[Option[A]]](Applicative[F].pure(None)) { head =>
      ApplicativeError[F, B].handleErrorWith(Functor[F].map(head) { value =>
        Option(value)
      }) { _ =>
        anyOf(values.tail)
      }
    }

  def retryWithDelay[F[_]: Sync: Timer, A](value: F[A], delay: FiniteDuration, retryCount: Int): F[A] =
    ApplicativeError[F, Throwable].handleErrorWith(value) { throwable =>
      if (retryCount == 0)
        Bracket[F, Throwable].raiseError(throwable)
      else
        Sync[F]
          .delay(println(s"Failed. $retryCount retries remaining. Trying again in $delay"))
          .productR(Timer[F].sleep(delay))
          .productR(retryWithDelay(value, delay, retryCount - 1))
    }

  def waitFor[F[_]: Sync: Timer](seconds: Int): F[Unit] =
    if (seconds == 0)
      Sync[F].delay(println("Waiting completed"))
    else
      Sync[F].delay(println(s"Waiting for $seconds seconds..."))
          .productR {
            Timer[F].sleep(1 second)
          }
          .productR(waitFor[F](seconds - 1))
}
