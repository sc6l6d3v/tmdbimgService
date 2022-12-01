package com.iscs.tmdbimg

import cats.effect.{ExitCode, IO, IOApp}
import com.iscs.tmdbimg.config.RedisConfig
import com.typesafe.scalalogging.Logger
import dev.profunktor.redis4cats.RedisCommands
import dev.profunktor.redis4cats.effect.Log.Stdout._
import sttp.client3.httpclient.fs2.HttpClientFs2Backend

object Main extends IOApp {
  private val L = Logger[this.type]

  val resources = for {
    redis       <- new RedisConfig[IO]().resource
    sttpRes     <- HttpClientFs2Backend.resource[IO]()
  } yield (redis, sttpRes)

  def run(args: List[String]): IO[ExitCode] = for {
    ec <- resources.use { case (cmd, sttpCli) =>
      implicit val redisCmd: RedisCommands[IO, String, String] = cmd
      for {
        _ <- IO.delay(L.info("starting service"))
        services <- TMDBServer.getServices(sttpCli)
        ec2 <- TMDBServer.getResource(services).use { _ => IO.never }
          .as(ExitCode.Success)
          .handleErrorWith(ex => IO {
            L.error("\"exception during stream startup\" exception={} ex={}", ex.toString, ex)
            ExitCode.Error
          })
      } yield ec2
    }
  } yield ec
}