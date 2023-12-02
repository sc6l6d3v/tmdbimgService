package com.iscs.tmdbimg.domains

import cats.effect.Sync
import cats.effect.kernel.Clock
import cats.implicits._
import com.iscs.tmdbimg.api.{FIND, POSTER, TMDBApiUri}
import com.iscs.tmdbimg.model.MediaTypes._
import com.iscs.tmdbimg.model.{EpisodeResults, MediaTypes, Meta, MovieResults, PersonResults, TVResults}
import com.typesafe.scalalogging.Logger
import dev.profunktor.redis4cats.RedisCommands
import fs2.Stream
import sttp.capabilities
import sttp.capabilities.fs2.Fs2Streams
import sttp.client3._
import sttp.model.{Uri, UriInterpolator}
import zio.json._

import java.util.Base64

trait TMDBImg[F[_]] extends Cache[F] {
  def getPoster(imdbid: String, size: String): Stream[F, Byte]
}

object TMDBImg extends UriInterpolator {
  private val L = Logger[this.type]
  private val B64Encoder = Base64.getEncoder

  def apply[F[_]](implicit ev: TMDBImg[F]): TMDBImg[F] = ev

  def fromMeta(ip: String): Option[Meta] = ip.fromJson[Meta] match {
    case Right(ip) => Some(ip)
    case Left(_) => None
  }

  def impl[F[_] : Sync](S: SttpBackend[F, Fs2Streams[F] with capabilities.WebSockets])
                       (implicit cmd: RedisCommands[F, String, String],
                        defImgMap: Map[String, Array[Byte]]): TMDBImg[F] = new TMDBImg[F] {

    def getMetaData(tmdbUri: Uri): F[Option[Meta]] = {
      basicRequest.get(tmdbUri).send(S).flatMap { responseEither =>
        responseEither.body match {
          case Left(error) =>
            L.warn(s"Request failed: $error")
            Sync[F].pure(Option.empty[Meta])
          case Right(bodyStr) =>
            L.info(s"got raw: $bodyStr")
            val decoded = keyMapping.foldLeft(bodyStr) { case (jsonStr, (k, v)) =>
              val pattern     = s""""$k":\\[\\{"""
              val replacement = s""""$k":\\[{"type":"$v","""
              jsonStr.replaceAll(pattern, replacement)
            }.fromJson[MediaTypes].left.map(new Exception(_))
            // Convert Either[String, MediaTypes] to Either[Throwable, MediaTypes]
            Sync[F].fromEither(decoded).map { mt =>
              if (mt.movie_results.nonEmpty) mt.movie_results.headOption
              else if (mt.person_results.nonEmpty) mt.person_results.headOption
              else if (mt.tv_results.nonEmpty) mt.tv_results.headOption
              else if (mt.tv_episode_results.nonEmpty) mt.tv_episode_results.headOption
              else if (mt.tv_season_results.nonEmpty) mt.tv_season_results.headOption
              else {
                L.warn(s"No data key=${tmdbUri.path.last}")
                Option.empty[Meta]
              }
            }
        }
      }
    }

    def getPosterData(tmdbPosterUri: Uri): F[Option[Array[Byte]]] = {
      for {
        imgBytesReq <- Sync[F].delay(quickRequest.contentType("image/jpeg").get(tmdbPosterUri).response(asByteArray))
        responseEither <- imgBytesReq.send(S)
        maybeBytes <- Sync[F].delay {
          responseEither.body.map { bodyStr =>
            L.info(s"got image: ${bodyStr.length}b")
            Some(bodyStr)
          }.getOrElse(Option.empty[Array[Byte]])
        }
      } yield maybeBytes
    }

    def getMeta(imdbid: String): F[Option[Meta]] = {
      for {
        key <- Sync[F].delay(s"tmdb:$imdbid")
        hasKey <- cmd.exists(key)
        tmdbExpr <- Sync[F].delay(TMDBApiUri.builder(TMDBApiUri(FIND, imdbid)))
        tmdbUri <- Sync[F].delay(uri"$tmdbExpr")
        resp <- if (!hasKey) {
          for {
            (getTime, tmdbMaybe) <- Clock[F].timed(getMetaData(tmdbUri))
            _ <- Sync[F].delay(L.info(s"got MetaData in {} ms", getTime.toMillis))
            _ <- tmdbMaybe match {
              case Some(tmdb) => setRedisKey(key, tmdb.toJson)
              case _          => Sync[F].unit
            }
          } yield tmdbMaybe
        } else {
          getMetaFromRedis(key)
        }
      } yield resp
    }

    def getPosterImg(posterPath: String, size: String): F[Option[Array[Byte]]] = {
      for {
        key <- Sync[F].delay(s"tmdbimg:$size:$posterPath")
        hasKey <- cmd.exists(key)
        tmdbExpr <- Sync[F].delay(TMDBApiUri.builder(TMDBApiUri(POSTER, posterPath), Some(size)))
        tmdbUri <- Sync[F].delay(uri"$tmdbExpr")
        resp <- if (!hasKey) {
          for {
            (getTime, tmdbMaybe) <- Clock[F].timed(getPosterData(tmdbUri))
            _ <- Sync[F].delay(L.info(s"got PosterData in {} ms", getTime.toMillis))
            _ <- tmdbMaybe match {
              case Some(tmdb) => setRedisKey(key, B64Encoder.encodeToString(tmdb)) //tmdb.map(_.toChar).mkString)
              case _          => Sync[F].unit
            }
          } yield tmdbMaybe
        } else
          getImgFromRedis(key)
      } yield resp
    }

    def meta2Poster(meta: Meta): F[Option[String]] = for {
      path <- Sync[F].delay {
        meta match {
          case movie: MovieResults => Some(movie.poster_path)
          case series: TVResults => Some(series.poster_path)
          case episode: EpisodeResults => Some(episode.still_path)
          case person: PersonResults => Some(person.known_for.head.poster_path)
          case _ => None
        }
      }
    } yield path

    def getPosterPath(imdbId: String): F[Option[String]] = for {
      mayBeMeta <- getMeta(imdbId)
      path <- mayBeMeta match {
        case Some(meta) => meta2Poster(meta)
        case _ => Sync[F].delay(Option.empty[String])
      }
    } yield path

    def getPosterEffect(imdbId: String, size: String): F[Option[Array[Byte]]] = for {
      maybePosterPath <- getPosterPath(imdbId)
      imgBytes <- maybePosterPath match {
        case Some(posterPath) => getPosterImg(posterPath, size)
        case _                => Sync[F].delay {
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
  }
}

