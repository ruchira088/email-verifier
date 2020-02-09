package com.ruchij.test

import java.util.concurrent.TimeUnit

import cats.{Applicative, Functor}
import cats.effect.{Clock, Sync}
import cats.implicits._
import org.joda.time.DateTime

import scala.concurrent.duration.TimeUnit

object Providers {

  implicit def clock[F[_]: Sync]: Clock[F] =
    stubClock {
      Sync[F].delay(DateTime.now())
    }

  def stubClock[F[_]: Applicative](dateTime: DateTime): Clock[F] =
    stubClock {
      Applicative[F].pure(dateTime)
    }

  def stubClock[F[_]: Functor](value: F[DateTime]): Clock[F] =
    new Clock[F] {
      override def realTime(unit: TimeUnit): F[Long] =
        value.map { dateTime => unit.convert(dateTime.getMillis, TimeUnit.MILLISECONDS) }

      override def monotonic(unit: TimeUnit): F[Long] = realTime(unit)
    }
}
