package com.ruchij.config

import cats.effect.Sync
import cats.~>
import pureconfig.ConfigObjectSource
import pureconfig.error.ConfigReaderException
import pureconfig.generic.auto._

case class ApplicationConfiguration(
  gmailConfiguration: GmailConfiguration,
  sendgridConfiguration: SendGridConfiguration
)

object ApplicationConfiguration {
  def load[F[_]: Sync](
    configObjectSource: ConfigObjectSource
  )(implicit functionK: Either[Throwable, *] ~> F): F[ApplicationConfiguration] =
    Sync[F].defer {
      functionK {
        configObjectSource
          .load[ApplicationConfiguration]
          .left
          .map(ConfigReaderException.apply[ApplicationConfiguration])
      }
    }
}
