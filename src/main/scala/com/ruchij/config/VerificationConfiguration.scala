package com.ruchij.config

import cats.effect.Sync
import cats.~>
import pureconfig.ConfigObjectSource
import pureconfig.error.ConfigReaderException
import pureconfig.generic.auto._

import scala.concurrent.duration.FiniteDuration

case class VerificationConfiguration(messagePeriod: FiniteDuration)

object VerificationConfiguration {

  def load[F[_]: Sync](configObjectSource: ConfigObjectSource)(implicit functionK: Either[Throwable, *] ~> F): F[VerificationConfiguration] =
    Sync[F].defer {
      functionK {
        configObjectSource.at("verification-configuration").load[VerificationConfiguration]
          .left
          .map(ConfigReaderException.apply[VerificationConfiguration])
      }
    }
}
