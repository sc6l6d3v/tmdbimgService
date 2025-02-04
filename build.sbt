import Dependencies._

lazy val root = (project in file("."))
  .settings(
    organization := "com.iscs",
    name := "tmdbimgService",
    version := "0.1-SNAPSHOT",
    scalaVersion := "2.13.16",
    scalacOptions ++= Seq("--release", "21"),
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
//      redis4cats.log4cats,
      specs2.test,
//      weaverTest.cats,
//      weaverTest.specs,
      logback.classic,
      logback.logging,
//      cats.retry,
//      cats.log4cats
    ),
//    testFrameworks += new TestFramework("weaver.framework.CatsEffect"),
    addCompilerPlugin("org.typelevel" %% "kind-projector"     % "0.13.3" cross CrossVersion.full),
    addCompilerPlugin("com.olegpy"    %% "better-monadic-for" % "0.3.1"),
    Revolver.enableDebugging(5051, suspend = true)
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

assembly / assemblyMergeStrategy := {
  case PathList("META-INF", "services", xs @ _*) => MergeStrategy.concat
  case PathList("META-INF", xs @ _*)             => MergeStrategy.discard
  case "io.netty.versions.properties"            => MergeStrategy.discard
  case "reference.conf"                          => MergeStrategy.concat
  // Keep or merge logback.xml instead of discarding it:
  case "logback.xml"                             => MergeStrategy.first
  case x                                         => MergeStrategy.first
}
