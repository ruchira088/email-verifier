import Dependencies._

lazy val root =
  (project in file("."))
    .enablePlugins(BuildInfoPlugin, JavaAppPackaging, SbtTwirl)
    .settings(
      name := "email-verifier",
      organization := "com.ruchij",
      scalaVersion := SCALA_VERSION,
      maintainer := "me@ruchij.com",
      libraryDependencies ++= rootDependencies ++ rootTestDependencies.map(_ % Test),
      buildInfoKeys := BuildInfoKey.ofN(name, organization, version, scalaVersion, sbtVersion),
      buildInfoPackage := "com.eed3si9n.ruchij",
      topLevelDirectory := None,
      scalacOptions ++= Seq("-Xlint", "-feature"),
      addCompilerPlugin(kindProjector),
      addCompilerPlugin(betterMonadicFor)
    )

lazy val rootDependencies =
  Seq(
    catsEffect,
    pureConfig,
    sendgrid,
    googleApiClient,
    googleOauthClientJetty,
    googleApiServicesGmail,
    shapeless,
    jodaTime,
    faker,
    fs2
  )

lazy val rootTestDependencies =
  Seq(scalaTest, pegdown)

addCommandAlias("testWithCoverage", "; coverage; test; coverageReport")
