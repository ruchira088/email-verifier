package com.ruchij.stubs

import cats.effect.Sync
import com.ruchij.services.email.EmailService
import com.ruchij.services.email.models.Email

import scala.collection.mutable

class InMemoryEmailService[F[_]: Sync](queue: mutable.Queue[Email]) extends EmailService[F] {
  override type Response = mutable.Queue[Email]

  override def send(email: Email): F[mutable.Queue[Email]] =
    Sync[F].delay {
      queue.enqueue(email)
    }

  def emails: List[Email] = queue.toList
}
