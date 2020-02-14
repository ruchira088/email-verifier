import Dependencies._

lazy val root =
  (project in file("."))
    .enablePlugins(BuildInfoPlugin, JavaAppPackaging, SbtTwirl)
    .settings(
      name := "email-verifier",
      organization := "com.ruchij",
      scalaVersion := SCALA_VERSION,
      maintainer := "me@ruchij.com",
      version := "0.0.1",
      libraryDependencies ++= rootDependencies ++ rootTestDependencies.map(_ % Test),
      buildInfoKeys := BuildInfoKey.ofN(name, organization, version, scalaVersion, sbtVersion),
      buildInfoPackage := "com.eed3si9n.ruchij",
      topLevelDirectory := None,
      scalacOptions ++= Seq("-Xlint", "-feature"),
      addCompilerPlugin(kindProjector),
      addCompilerPlugin(betterMonadicFor),
      assemblyJarName in assembly := "email-verifier.jar",
      assemblyMergeStrategy in assembly := {
        case PathList("META-INF", _*) => MergeStrategy.discard
        case _ => MergeStrategy.first
      }
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
    http4sDsl,
    http4sBlazeClient,
    http4sCirce,
    circeGeneric,
    awsLambdaJavaCore,
    awsLambdaJavaEvents
  )

lazy val rootTestDependencies =
  Seq(scalaTest, pegdown)

addCommandAlias("testWithCoverage", "; coverage; test; coverageReport")
