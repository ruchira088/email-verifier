package com.ruchij.services.email

import com.ruchij.services.email.models.Email

trait EmailService[F[_]] {
  type Response

  def send(email: Email): F[Response]
}
