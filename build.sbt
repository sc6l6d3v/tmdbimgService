import Dependencies._

lazy val root = (project in file("."))
  .settings(
    organization := "com.iscs",
    name := "tmdbimgService",
    version := "0.1-SNAPSHOT",
    scalaVersion := "2.13.8",
    scalacOptions ++= Seq("-target:17"),
    libraryDependencies ++= Seq(
      http4s.blaze_server,
      http4s.dsl,
      http4s.server,
      sttp.client3,
      zio.json,
      zio.test,
      zio.test_sbt,
      zio.test_magnolia,
      redis4cats.core,
      redis4cats.stream,
      redis4cats.log4cats,
      specs2.test,
      weaverTest.cats,
      weaverTest.specs,
      logback.classic,
      logback.logging,
      cats.retry,
      cats.log4cats
    ),
    testFrameworks += new TestFramework("weaver.framework.CatsEffect"),
    addCompilerPlugin("org.typelevel" %% "kind-projector"     % "0.10.3"),
    addCompilerPlugin("com.olegpy"    %% "better-monadic-for" % "0.3.1"),
    Revolver.enableDebugging(5050, true)
  )

scalacOptions ++= Seq(
//  "-Ylog-classpath",
  "-deprecation",
  "-encoding", "UTF-8",
  "-language:higherKinds",
  "-language:postfixOps",
  "-feature"
  //"-Xfatal-warnings",
)

assemblyMergeStrategy in assembly := {
  case PathList("META-INF", _ @ _*) => MergeStrategy.discard
  case _ => MergeStrategy.first
}
