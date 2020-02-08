package com.ruchij.config

import cats.effect.Sync
import cats.~>
import pureconfig.ConfigObjectSource
import pureconfig.error.ConfigReaderException
import pureconfig.generic.auto._

case class SendGridConfiguration(apiKey: String)

object SendGridConfiguration {

  def load[F[_]: Sync](
    configObjectSource: ConfigObjectSource
  )(implicit functionK: Either[Throwable, *] ~> F): F[SendGridConfiguration] =
    Sync[F].defer {
      functionK {
        configObjectSource
          .at("sendgrid-configuration")
          .load[SendGridConfiguration]
          .left
          .map(ConfigReaderException.apply[SendGridConfiguration])
      }
    }
}
