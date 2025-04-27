package com.iscs.tmdbimg.domains

import cats.effect.Sync
import cats.effect.kernel.Clock
import cats.implicits.*
import com.iscs.tmdbimg.api.{FIND, POSTER, TMDBApiUri}
import com.iscs.tmdbimg.model.MediaTypes.*
import com.iscs.tmdbimg.model.*
import com.typesafe.scalalogging.Logger
import dev.profunktor.redis4cats.RedisCommands
import fs2.Stream
import sttp.capabilities
import sttp.capabilities.fs2.Fs2Streams
import sttp.client3.*
import sttp.model.{Uri, UriInterpolator}
import zio.json.*

import java.util.Base64

trait TMDBImg[F[_]] extends Cache[F] {
  def getPoster(imdbid: String, size: String): Stream[F, Byte]
  def getPath(imdbid: String, size: String): F[OnlyPath]
}

object TMDBImg extends UriInterpolator {
  private val L          = Logger[this.type]
  private val B64Encoder = Base64.getEncoder

  def apply[F[_]](implicit ev: TMDBImg[F]): TMDBImg[F] = ev

  def fromMeta(ip: String): Option[Meta] = ip.fromJson[Meta] match {
    case Right(ip) => Some(ip)
    case Left(_)   => None
  }

  def impl[F[_]: Sync](
      S: SttpBackend[F, Fs2Streams[F] & capabilities.WebSockets]
  )(implicit cmd: RedisCommands[F, String, String], defImgMap: Map[String, Array[Byte]], defPathMap: Map[String, String]): TMDBImg[F] =
    new TMDBImg[F] {

      def getMetaData(tmdbUri: Uri): F[Option[Meta]] =
        basicRequest.get(tmdbUri).send(S).flatMap { responseEither =>
          responseEither.body match {
            case Left(error) =>
              L.warn(s"Request failed: $error")
              Sync[F].pure(Option.empty[Meta])
            case Right(bodyStr) =>
              L.info(s"got raw: $bodyStr")
              val decoded = keyMapping
                .foldLeft(bodyStr) { case (jsonStr, (k, v)) =>
                  val pattern     = s""""$k":\\[\\{"""
                  val replacement = s""""$k":\\[{"type":"$v","""
                  jsonStr.replaceAll(pattern, replacement)
                }
                .fromJson[MediaTypes]
                .left
                .map(new Exception(_)) // Convert Either[String, MediaTypes] to Either[Throwable, MediaTypes]
              Sync[F].fromEither(decoded).map { mt =>
                List(mt.movie_results, mt.person_results, mt.tv_results, mt.tv_episode_results, mt.tv_season_results).flatten.headOption
                  .orElse {
                    L.warn(s"No data key=${tmdbUri.path.last}")
                    Option.empty[Meta]
                  }
              }
          }
        }

      def getPosterData(tmdbPosterUri: Uri): F[Option[Array[Byte]]] =
        for {
          imgBytesReq    <- Sync[F].delay(quickRequest.contentType("image/jpeg").get(tmdbPosterUri).response(asByteArray))
          responseEither <- imgBytesReq.send(S)
          maybeBytes <- Sync[F].delay {
            responseEither.body
              .map { bodyStr =>
                L.info(s"got image: ${bodyStr.length}b")
                Some(bodyStr)
              }
              .getOrElse(Option.empty[Array[Byte]])
          }
        } yield maybeBytes

      def getMeta(imdbid: String): F[Option[Meta]] =
        for {
          key      <- Sync[F].delay(s"tmdb:$imdbid")
          hasKey   <- cmd.exists(key)
          tmdbExpr <- Sync[F].delay(TMDBApiUri.builder(TMDBApiUri(FIND, imdbid)))
          tmdbUri  <- Sync[F].delay(uri"$tmdbExpr")
          resp <-
            if (!hasKey) {
              for {
                (getTime, metaMaybe) <- Clock[F].timed(getMetaData(tmdbUri))
                _                    <- Sync[F].delay(L.info(s"got fresh MetaData in {} ms", getTime.toMillis))
                _ <- metaMaybe match {
                  case Some(tmdb) => checkSetMeta(tmdb, key)
                  case _          => Sync[F].unit
                }
              } yield metaMaybe
            } else
              for {
                (getTime, redisMetaMaybe) <- Clock[F].timed(getMetaFromRedis(key))
                _                         <- Sync[F].delay(L.info(s"got cached MetaData in {} ms", getTime.toMillis))
              } yield redisMetaMaybe
        } yield resp

      def getPosterImg(posterPath: String, size: String): F[Option[Array[Byte]]] =
        for {
          key      <- Sync[F].delay(s"tmdbimg:$size:$posterPath")
          hasKey   <- cmd.exists(key)
          tmdbExpr <- Sync[F].delay(TMDBApiUri.builder(TMDBApiUri(POSTER, posterPath), Some(size)))
          tmdbUri  <- Sync[F].delay(uri"$tmdbExpr")
          resp <-
            if (!hasKey) {
              for {
                (getTime, tmdbMaybe) <- Clock[F].timed(getPosterData(tmdbUri))
                _                    <- Sync[F].delay(L.info(s"got fresh PosterData in {} ms", getTime.toMillis))
                _ <- tmdbMaybe match {
                  case Some(tmdb) => setRedisKey(key, B64Encoder.encodeToString(tmdb))
                  case _          => Sync[F].unit
                }
              } yield tmdbMaybe
            } else
              for {
                (getTime, redisImgMaybe) <- Clock[F].timed(getImgFromRedis(key))
                _                        <- Sync[F].delay(L.info(s"got cached PosterData in {} ms", getTime.toMillis))
              } yield redisImgMaybe
        } yield resp

      def meta2Poster(meta: Meta): F[Option[String]] = for {
        path <- Sync[F].delay {
          meta match {
            case movie: MovieResults     => movie.poster_path
            case series: TVResults       => series.poster_path
            case episode: EpisodeResults => episode.still_path
            case person: PersonResults   => person.known_for.head.poster_path
            case null                    => None
          }
        }
      } yield path

      def checkSetMeta(tmdb: Meta, key: String): F[Unit] =
        meta2Poster(tmdb).flatMap {
          case Some(_) =>
            Sync[F].delay(L.info(s"get Meta with path")) *> setRedisKey(key, tmdb.toJson)
          case _ =>
            Sync[F].delay(L.info(s"got Meta without path"))
        }

      def getPosterPath(imdbId: String): F[Option[String]] = for {
        mayBeMeta <- getMeta(imdbId)
        path <- mayBeMeta match {
          case Some(meta) => meta2Poster(meta)
          case _          => Sync[F].delay(Option.empty[String])
        }
      } yield path

      def getPosterEffect(imdbId: String, size: String): F[Option[Array[Byte]]] = for {
        maybePosterPath <- getPosterPath(imdbId)
        imgBytes <- maybePosterPath match {
          case Some(posterPath) => getPosterImg(posterPath, size)
          case _ =>
            Sync[F].delay {
              L.warn(s"Using default $size image")
              Some(defImgMap(size))
            }
        }
      } yield imgBytes

      override def getPoster(imdbId: String, size: String): Stream[F, Byte] = for {
        maybeBytes <- Stream.eval(getPosterEffect(imdbId, size))
        bytes <- maybeBytes match {
          case Some(imgBytes) => Stream.emits(imgBytes)
          case _              => Stream.empty
        }
      } yield bytes

      override def getPath(imdbid: String, size: String): F[OnlyPath] = for {
        maybePosterPath <- getPosterPath(imdbid)
      } yield OnlyPath(maybePosterPath.getOrElse(defPathMap(size)))
    }
}
