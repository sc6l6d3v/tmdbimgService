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
      case _ @ GET -> "meta" /: imdbKey  =>
        Ok(for {
          pathParts <- Stream.eval(Sync[F].delay(imdbKey.segments.toList))
          _ <- Stream.eval(Sync[F].delay(L.info(s""""meta request" key=$imdbKey size=${pathParts.head}""")))
          resp <- C.getPoster(pathParts.head.encoded,
          if (pathParts.size == 2)
            pathParts.tail.head.encoded
          else
            "S")
        } yield resp)
    }
  }
}
