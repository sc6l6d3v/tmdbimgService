import Dependencies.*

ThisBuild / version              := "1.0"
ThisBuild / scalaVersion         := "3.4.2"
ThisBuild / organization         := "com.iscs"
ThisBuild / organizationName     := "iscs"
ThisBuild / organizationHomepage := Some(url("https://github.com/sc6l6d3v"))
ThisBuild / scalacOptions ++= Seq("--release", "21")

lazy val root = (project in file("."))
  .settings(
    name := "tmdbimgService",
    libraryDependencies ++= Seq(
      http4s.dsl,
      http4s.server,
      sttp.client3,
      zio.json,
      zio.test,
      zio.test_sbt,
      zio.test_magnolia,
      redis4cats.core,
      redis4cats.stream,
      specs2.test,
//      weaverTest.cats,
//      weaverTest.specs,
      logback.classic,
      logback.logging
    ),
//    testFrameworks += new TestFramework("weaver.framework.CatsEffect"),
    Revolver.enableDebugging(5051, suspend = true)
  )

scalacOptions ++= Seq(
//  "-Ylog-classpath",
  "-deprecation",
  "-encoding",
  "UTF-8",
  "-language:higherKinds",
  "-language:postfixOps",
  "-feature",
  "-Ykind-projector"
  // "-Xfatal-warnings",
)

addCommandAlias(
  "format",
  "scalafmt; scalafmtSbt; Test / scalafmt"
)

addCommandAlias(
  "formatCheck",
  "scalafmtCheck; scalafmtSbtCheck; Test  / scalafmtCheck"
)

addCommandAlias(
  "validate",
  "formatCheck; coverage; test; coverageReport; coverageAggregate; coverageOff"
)

assembly / assemblyMergeStrategy := {
  case PathList("META-INF", "services", xs @ _*) => MergeStrategy.concat
  case PathList("META-INF", xs @ _*)             => MergeStrategy.discard
  case "io.netty.versions.properties"            => MergeStrategy.discard
  case "reference.conf"                          => MergeStrategy.concat
  // Keep or merge logback.xml instead of discarding it:
  case "logback.xml" => MergeStrategy.first
  case x             => MergeStrategy.first
}
