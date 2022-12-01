package com.iscs.tmdbimg.domains

import cats.effect.Sync
import cats.implicits._
import com.iscs.tmdbimg.domains.TMDBImg.fromMeta
import com.iscs.tmdbimg.model.Meta
import com.typesafe.scalalogging.Logger
import dev.profunktor.redis4cats.RedisCommands
import scala.concurrent.duration.DurationInt

trait Cache[F[_]] {
  private val L = Logger[this.type]
  private val expiryTime = 15.minutes

  def getMetaFromRedis[S[_]: Sync](key: String)(implicit cmd: RedisCommands[S, String, String]): S[Option[Meta]] = for {
    strOpt <- cmd.get(key)
    retrieved <- Sync[S].delay(strOpt.flatMap { str =>
      L.info("\"retrieved key\" key={} value={}", key, str)
      fromMeta(str)
    })
  } yield retrieved

  def getImgFromRedis[S[_]: Sync](key: String)(implicit cmd: RedisCommands[S, String, String]): S[Option[Array[Byte]]] = for {
    strOpt <- cmd.get(key)
    retrieved <- Sync[S].delay(strOpt.flatMap { str =>
      L.info("\"retrieved key\" key={} value={}", key, str)
      Some(str.getBytes)
    })
  } yield retrieved

  def setRedisKey[S[_]: Sync](key: String, inpValue: String)(
    implicit cmd: RedisCommands[S, String, String]): S[Unit] = for {
    asString <- Sync[S].delay(inpValue)
    _ <- Sync[S].delay(L.info("\"setting key\" key={} value={}", key, asString))
    _ <- cmd.setEx(key, asString, expiryTime)
  } yield ()
}
