import sbt._

object Dependencies
{
  val SCALA_VERSION = "2.13.1"
  val HTTP4S_VERSION = "0.21.0-RC4"

  lazy val kindProjector = "org.typelevel" %% "kind-projector" % "0.11.0" cross CrossVersion.full

  lazy val betterMonadicFor = "com.olegpy" %% "better-monadic-for" % "0.3.1"

  lazy val catsEffect = "org.typelevel" %% "cats-effect" % "2.1.1"

  lazy val pureConfig = "com.github.pureconfig" %% "pureconfig" % "0.12.2"

  lazy val sendgrid = "com.sendgrid" % "sendgrid-java" % "4.4.4"

  lazy val googleApiClient = "com.google.api-client" % "google-api-client" % "1.23.0"

  lazy val googleOauthClientJetty = "com.google.oauth-client" % "google-oauth-client-jetty" % "1.23.0"

  lazy val googleApiServicesGmail = "com.google.apis" % "google-api-services-gmail" % "v1-rev83-1.23.0"

  lazy val shapeless =  "com.chuusai" %% "shapeless" % "2.3.3"

  lazy val jodaTime = "joda-time" % "joda-time" % "2.10.5"

  lazy val faker = "com.github.javafaker" % "javafaker" % "1.0.1"

  lazy val http4sDsl = "org.http4s" %% "http4s-dsl" % HTTP4S_VERSION

  lazy val http4sBlazeClient = "org.http4s" %% "http4s-blaze-client" % HTTP4S_VERSION

  lazy val http4sCirce = "org.http4s" %% "http4s-circe" % HTTP4S_VERSION

  lazy val circeGeneric = "io.circe" %% "circe-generic" % "0.13.0"

  lazy val scalaTest = "org.scalatest" %% "scalatest" % "3.1.0"

  lazy val pegdown = "org.pegdown" % "pegdown" % "1.6.0"
}
