package com.ruchij.config

import cats.effect.Sync
import cats.~>
import com.ruchij.services.email.models.Email.EmailAddress
import com.ruchij.types.ConfigReaderTypes.emailConfigReader
import pureconfig.{ConfigObjectSource, ConfigReader}
import pureconfig.error.ConfigReaderException
import pureconfig.generic.auto._

case class SendGridConfiguration(apiKey: String, sender: EmailAddress, destination: EmailAddress)

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
