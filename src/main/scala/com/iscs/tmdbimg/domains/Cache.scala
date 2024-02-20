package com.iscs.tmdbimg.domains

import cats.effect.Sync
import cats.effect.kernel.Clock
import cats.implicits._
import com.iscs.tmdbimg.domains.TMDBImg.fromMeta
import com.iscs.tmdbimg.model.Meta
import com.typesafe.scalalogging.Logger
import dev.profunktor.redis4cats.RedisCommands
import java.util.Base64
import scala.concurrent.duration.DurationInt

trait Cache[F[_]] {
  private val L = Logger[this.type]
  private val B64decoder = Base64.getDecoder
  private val expiryTime = 15.minutes

  def getMetaFromRedis[S[_]: Sync](key: String)(implicit cmd: RedisCommands[S, String, String]): S[Option[Meta]] = for {
    (getTime, strOpt) <- Clock[S].timed(cmd.get(key))
    retrieved <- Sync[S].delay(strOpt.flatMap { str =>
      L.info("\"retrieved key\" key={} value={}... ms={}", key, str.take(20), getTime.toMillis)
      fromMeta(str)
    })
  } yield retrieved

  def getImgFromRedis[S[_]: Sync](key: String)(implicit cmd: RedisCommands[S, String, String]): S[Option[Array[Byte]]] = for {
    (getTime, strOpt) <- Clock[S].timed(cmd.get(key))
    retrieved <- Sync[S].delay(strOpt.flatMap { str =>
      L.info("\"retrieved key\" key={} value={}... ms={}", key, str.take(20), getTime.toMillis)
      Some(B64decoder.decode(str))
    })
  } yield retrieved

  def setRedisKey[S[_]: Sync](key: String, inpValue: String)(
    implicit cmd: RedisCommands[S, String, String]): S[Unit] = for {
    (getTime, _) <- Clock[S].timed(cmd.setEx(key, inpValue, expiryTime))
    _ <- Sync[S].delay(L.info("\"setting key\" key={} value={}... ms={}", key, inpValue.take(20), getTime.toMillis))
  } yield ()
}
