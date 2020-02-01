import sbt._

object Dependencies
{
  val SCALA_VERSION = "2.13.1"

  lazy val kindProjector = "org.typelevel" %% "kind-projector" % "0.11.0" cross CrossVersion.full

  lazy val catsEffect = "org.typelevel" %% "cats-effect" % "2.0.0"

  lazy val pureConfig = "com.github.pureconfig" %% "pureconfig" % "0.12.2"

  lazy val sendgrid = "com.sendgrid" % "sendgrid-java" % "4.4.1"

  lazy val googleApiClient = "com.google.api-client" % "google-api-client" % "1.23.0"

  lazy val googleOauthClientJetty = "com.google.oauth-client" % "google-oauth-client-jetty" % "1.23.0"

  lazy val googleApiServicesGmail = "com.google.apis" % "google-api-services-gmail" % "v1-rev83-1.23.0"

  lazy val shapeless =  "com.chuusai" %% "shapeless" % "2.3.3"

  lazy val scalaTest = "org.scalatest" %% "scalatest" % "3.1.0"

  lazy val pegdown = "org.pegdown" % "pegdown" % "1.6.0"
}
