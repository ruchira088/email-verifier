package com.ruchij.services.gmail

import java.io.StringReader
import java.util.Collections

import cats.Applicative
import cats.effect.{Blocker, ContextShift, Sync}
import cats.implicits._
import com.eed3si9n.ruchij.BuildInfo
import com.google.api.client.auth.oauth2.StoredCredential
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver
import com.google.api.client.googleapis.auth.oauth2.{GoogleAuthorizationCodeFlow, GoogleClientSecrets}
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.client.util.store.MemoryDataStoreFactory
import com.google.api.services.gmail.{Gmail, GmailScopes}
import com.ruchij.config.GmailConfiguration
import com.ruchij.services.email.models.Email.EmailAddress
import com.ruchij.services.gmail.GmailServiceImpl.AUTHENTICATED_USER
import com.ruchij.services.gmail.models.{GmailMessage, GmailMessagesResult}

import scala.jdk.CollectionConverters._

class GmailServiceImpl[F[_]: Sync: ContextShift](gmail: Gmail, blocker: Blocker) extends GmailService[F] {

  override def fetchMessages(from: EmailAddress, pageToken: Option[String]): F[GmailMessagesResult] =
    blocker
      .delay {
        pageToken.fold(
          gmail
            .users()
            .messages()
            .list(AUTHENTICATED_USER)
            .setMaxResults(10L)
            .setQ(s"from:$from")
            .execute()
        ) { token =>
          gmail
            .users()
            .messages()
            .list(AUTHENTICATED_USER)
            .setMaxResults(10L)
            .setQ(s"from:$from")
            .setPageToken(token)
            .execute()
        }
      }
      .flatMap { listMessagesResponse =>
        listMessagesResponse.getMessages.asScala.toList
          .traverse { message =>
            blocker
              .delay {
                gmail.users().messages().get(AUTHENTICATED_USER, message.getId).execute()
              }
              .flatMap(GmailMessage.parse[F])
          }
          .map { messages =>
            GmailMessagesResult(messages, Option(listMessagesResponse.getNextPageToken))
          }
      }

  override def deleteMessage(messageId: String): F[Unit] =
    blocker
      .delay {
        gmail.users().messages().trash(AUTHENTICATED_USER, messageId).execute()
      }
      .productR(Applicative[F].unit)
}

object GmailServiceImpl {
  val AUTHENTICATED_USER = "me"

  def create[F[_]: Sync: ContextShift](
    gmailConfiguration: GmailConfiguration,
    blocker: Blocker
  ): F[GmailServiceImpl[F]] = {
    val jacksonFactory = JacksonFactory.getDefaultInstance

    for {
      httpTransport <- Sync[F].delay(GoogleNetHttpTransport.newTrustedTransport())

      clientSecrets <- Sync[F].delay {
        GoogleClientSecrets.load(
          jacksonFactory,
          new StringReader(StringContext.processEscapes(gmailConfiguration.credentials))
        )
      }

      storedCredential = new StoredCredential().setRefreshToken(gmailConfiguration.refreshToken)

      memoryDataStore <- Sync[F].delay {
        MemoryDataStoreFactory.getDefaultInstance
          .getDataStore[StoredCredential](StoredCredential.DEFAULT_DATA_STORE_ID)
          .set("user", storedCredential)
      }

      googleAuthorizationCodeFlow = new GoogleAuthorizationCodeFlow.Builder(
        httpTransport,
        jacksonFactory,
        clientSecrets,
        Collections.singletonList(GmailScopes.MAIL_GOOGLE_COM)
      ).setCredentialDataStore(memoryDataStore)
        .setAccessType("offline")
        .build()

      localServerReceiver = new LocalServerReceiver.Builder().setPort(-1).build()

      authorizationCodeInstalledApp <- Sync[F].delay {
        new AuthorizationCodeInstalledApp(googleAuthorizationCodeFlow, localServerReceiver)
          .authorize("user")
      }

      gmail <- Sync[F].delay {
        new Gmail.Builder(httpTransport, jacksonFactory, authorizationCodeInstalledApp)
          .setApplicationName(BuildInfo.name)
          .build()
      }

    } yield new GmailServiceImpl[F](gmail, blocker)
  }
}
