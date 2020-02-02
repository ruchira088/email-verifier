package com.ruchij.config

import cats.effect.Sync
import cats.~>
import com.ruchij.services.email.models.Email.EmailAddress
import com.ruchij.types.ConfigReaderTypes.emailConfigReader
import pureconfig.ConfigObjectSource
import pureconfig.error.ConfigReaderException
import pureconfig.generic.auto._

case class GmailConfiguration(refreshToken: String, credentials: String, sender: EmailAddress)

object GmailConfiguration {

  def load[F[_]: Sync](
    configObjectSource: ConfigObjectSource
  )(implicit functionK: Either[Throwable, *] ~> F): F[GmailConfiguration] =
    Sync[F].defer {
      functionK {
        configObjectSource
          .at("gmail-configuration")
          .load[GmailConfiguration]
          .left
          .map(ConfigReaderException.apply[GmailConfiguration])
      }
    }
}
