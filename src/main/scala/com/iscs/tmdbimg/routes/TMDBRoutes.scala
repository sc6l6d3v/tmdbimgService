package com.iscs.tmdbimg.routes

import cats.effect.Sync
import cats.implicits._
import com.iscs.tmdbimg.domains.TMDBImg
import com.typesafe.scalalogging.Logger
import fs2.Stream
import org.http4s.{CacheDirective, EntityEncoder, HttpRoutes, MediaType}
import org.http4s.dsl.Http4sDsl
import org.http4s.headers._
import zio.json.{EncoderOps, JsonEncoder}
import scala.concurrent.duration._

object TMDBRoutes {
  private val L = Logger[this.type]

  implicit def jsonEncoderOf[F[_], A](implicit encoder: JsonEncoder[A], F: Sync[F]): EntityEncoder[F, A] =
    EntityEncoder
      .stringEncoder[F]
      .contramap[A](_.toJson) // Convert A to JSON string using zio.json
      .withContentType(`Content-Type`(MediaType.application.json))

  def TmdbRoutes[F[_]: Sync](C: TMDBImg[F]): HttpRoutes[F] = {
    val dsl = new Http4sDsl[F] {}
    import dsl._
    HttpRoutes.of[F] {
      case _ @GET -> "meta" /: imdbKey =>
        Ok(for {
          pathParts <- Stream.eval(Sync[F].delay(imdbKey.segments.toList))
          _         <- Stream.eval(Sync[F].delay(L.info(s""""meta request" key=$imdbKey size=${pathParts.last.encoded}""")))
          resp      <- C.getPoster(pathParts.head.encoded, if (pathParts.size == 2) pathParts.tail.head.encoded else "S")
        } yield resp)
          .map(
            _.putHeaders(
              `Cache-Control`(
                CacheDirective.`public`,
                CacheDirective.`max-age`(86400 seconds) // 24 hours
              )
            )
          )
      case _ @GET -> "path" /: imdbKey =>
        Ok(for {
          pathParts <- Sync[F].delay(imdbKey.segments.toList)
          _         <- Sync[F].delay(L.info(s""""path request" key=$imdbKey size=${pathParts.head}"""))
          respObj   <- C.getPath(pathParts.head.encoded, if (pathParts.size == 2) pathParts.tail.head.encoded else "S")
        } yield respObj)
    }
  }
}
