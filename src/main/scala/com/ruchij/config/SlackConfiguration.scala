package com.ruchij.config

import cats.effect.Sync
import cats.~>
import pureconfig.ConfigObjectSource
import pureconfig.error.ConfigReaderException
import pureconfig.generic.auto._

case class SlackConfiguration(generalChannelSecret: String, emailVerifierChannelSecret: String)

object SlackConfiguration {

  def load[F[_]: Sync](
    configObjectSource: ConfigObjectSource
  )(implicit functionK: Either[Throwable, *] ~> F): F[SlackConfiguration] =
    Sync[F].suspend {
      functionK {
        configObjectSource
          .at("slack-configuration")
          .load[SlackConfiguration]
          .left
          .map(ConfigReaderException.apply)
      }
    }
}
