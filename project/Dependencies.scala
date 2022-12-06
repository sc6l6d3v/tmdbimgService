import sbt._

object Dependencies {
  object Versions {
    val Http4sVersion = "0.23.12"
    val Specs2Version = "4.16.1"
    val LogbackVersion = "1.2.3"
    val catsRetryVersion = "1.1.0"
    val log4catsVersion = "2.3.1"
    val fs2Version = "3.3.0"
    val loggingVersion = "3.9.2"
    val redis4catsVersion = "1.2.0"
    val zioJsonVersion = "0.3.0"
    val zioTestVersion = "2.0.4"
    val sttpVersion = "3.8.3"
    val WeaverTestVersion = "0.7.15"
  }

  object http4s {
    val server       = "org.http4s"  %% "http4s-ember-server" % Versions.Http4sVersion
    val blaze_server = "org.http4s"  %% "http4s-blaze-server" % Versions.Http4sVersion
    val dsl          = "org.http4s"  %% "http4s-dsl"          % Versions.Http4sVersion
  }

  object sttp {
    val client3 = "com.softwaremill.sttp.client3" %% "fs2" % Versions.sttpVersion
  }

  object zio {
    val json = "dev.zio" %% "zio-json" % Versions.zioJsonVersion
    val test = "dev.zio" %% "zio-test" % Versions.zioTestVersion
    val test_sbt = "dev.zio" %% "zio-test-sbt" % Versions.zioTestVersion
    val test_magnolia = "dev.zio" %% "zio-test-magnolia" % Versions.zioTestVersion
  }

  object redis4cats {
    val core     = "dev.profunktor" %% "redis4cats-effects"  % Versions.redis4catsVersion
    val stream   = "dev.profunktor" %% "redis4cats-streams"  % Versions.redis4catsVersion
    val log4cats = "dev.profunktor" %% "redis4cats-log4cats" % Versions.redis4catsVersion
  }

  object weaverTest {
    val cats  = "com.disneystreaming" %% "weaver-cats"      % Versions.WeaverTestVersion % "test"
    val specs = "com.disneystreaming" %% "weaver-specs2"    % Versions.WeaverTestVersion % "test"
  }

  object specs2 {
    val test = "org.specs2"       %% "specs2-core"         % Versions.Specs2Version % "test"
  }

  object logback {
    val classic = "ch.qos.logback"             %  "logback-classic" % Versions.LogbackVersion
    val logging = "com.typesafe.scala-logging" %% "scala-logging"   % Versions.loggingVersion
  }

  object cats {
    val retry = "com.github.cb372" %% "cats-retry"      % Versions.catsRetryVersion
    val log4cats = "org.typelevel" %% s"log4cats-slf4j" % Versions.log4catsVersion
  }
}
