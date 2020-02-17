package com.ruchij.types

import com.ruchij.services.email.models.Email
import com.ruchij.services.email.models.Email.EmailAddress
import org.joda.time.DateTimeZone
import pureconfig.ConfigReader

import scala.util.Try

object ConfigReaderTypes {

  implicit val emailConfigReader: ConfigReader[EmailAddress] =
    ConfigReader.fromNonEmptyString(string => Right(Email.lift(string)))

  implicit val dateTimeZoneConfigReader: ConfigReader[DateTimeZone] =
    ConfigReader.fromNonEmptyStringTry {
      string => Try(DateTimeZone.forID(string))
    }
}
