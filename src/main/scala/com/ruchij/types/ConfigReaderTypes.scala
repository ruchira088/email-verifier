package com.ruchij.types

import com.ruchij.services.email.models.Email
import com.ruchij.services.email.models.Email.EmailAddress
import pureconfig.ConfigReader

object ConfigReaderTypes {

  implicit val emailConfigReader: ConfigReader[EmailAddress] =
    ConfigReader.fromNonEmptyString(string => Right(Email.lift(string)))
}
