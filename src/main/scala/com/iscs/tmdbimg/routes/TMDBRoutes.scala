package com.iscs.tmdbimg.routes

import cats.effect.Sync
import com.iscs.tmdbimg.domains.TMDBImg
import com.typesafe.scalalogging.Logger
import fs2.Stream
import org.http4s._
import org.http4s.dsl.Http4sDsl

object TMDBRoutes {
  private val L = Logger[this.type]

  def TmdbRoutes[F[_]: Sync](C: TMDBImg[F]): HttpRoutes[F] = {
    val dsl = new Http4sDsl[F]{}
    import dsl._
    HttpRoutes.of[F] {
      case _ @ GET -> Root / "meta" / imdbKey =>
        Ok(for {
          _ <- Stream.eval(Sync[F].delay(L.info(s""""meta request" $imdbKey""")))
          resp <- C.getPoster(imdbKey.toLowerCase)
        } yield resp)
    }
  }
}
