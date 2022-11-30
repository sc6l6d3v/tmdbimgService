package com.iscs.tmdbimg.routes

import cats.effect.Sync
import cats.implicits._
import com.iscs.tmdbimg.domains.TMDBImg
import com.typesafe.scalalogging.Logger
import zio.json._
import org.http4s._
import org.http4s.dsl.Http4sDsl

object TMDBRoutes {
  private val L = Logger[this.type]

  implicit val llSEncoder: JsonEncoder[List[List[String]]] = DeriveJsonEncoder.gen[List[List[String]]]

  def TmdbRoutes[F[_]: Sync](C: TMDBImg[F]): HttpRoutes[F] = {
    val dsl = new Http4sDsl[F]{}
    import dsl._
    HttpRoutes.of[F] {
      case _ @ GET -> Root / "meta" / imdbKey =>
        for {
          metaJson <- C.getPosterPath(imdbKey.toLowerCase)
          _ <- Sync[F].delay(L.info(s""""meta request" $imdbKey"""))
          resp <- Ok(metaJson.toJson)
        } yield resp
    }
  }
}
