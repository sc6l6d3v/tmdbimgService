package com.iscs.tmdbimg

import cats.effect.{Async, Resource, Sync}
import cats.implicits._
import com.comcast.ip4s._
import com.iscs.tmdbimg.domains.TMDBImg
import com.iscs.tmdbimg.routes.TMDBRoutes
import com.typesafe.scalalogging.Logger
import dev.profunktor.redis4cats.RedisCommands
import org.http4s.HttpApp
import org.http4s.ember.server._
import org.http4s.implicits._
import org.http4s.server.middleware.{Logger => hpLogger}
import org.http4s.server.{Router, Server}
import sttp.capabilities
import sttp.capabilities.fs2.Fs2Streams
import sttp.client3.SttpBackend

object TMDBServer {
  private val port = sys.env.getOrElse("PORT", "8080").toInt
  private val bindHost = sys.env.getOrElse("BINDHOST", "0.0.0.0")
  private val serverPoolSize = sys.env.getOrElse("SERVERPOOL", "16").toInt

  private val L = Logger[this.type]

  def getServices[F[_]: Async](sttpClient: SttpBackend[F, Fs2Streams[F] with capabilities.WebSockets])
                              (implicit cmd: RedisCommands[F, String, String]): F[HttpApp[F]] = {
    for {
      geoip   <- Sync[F].delay(TMDBImg.impl[F](sttpClient))
      httpApp <- Sync[F].delay(
        Router("/" -> TMDBRoutes.TmdbRoutes[F](geoip))
          .orNotFound)
      finalHttpApp <- Sync[F].delay(hpLogger.httpApp(logHeaders = true, logBody = true)(httpApp))
    } yield finalHttpApp
  }

  def getResource[F[_]: Async](finalHttpApp: HttpApp[F]): Resource[F, Server] = {
    for {
      server <- EmberServerBuilder
        .default[F]
        .withHost(Ipv4Address.fromString(bindHost).getOrElse(ipv4"0.0.0.0"))
        .withPort(Port.fromInt(port).getOrElse(port"8080"))
        .withHttpApp(finalHttpApp)
        .withMaxConnections(serverPoolSize)
        .build
    } yield server
  }
}