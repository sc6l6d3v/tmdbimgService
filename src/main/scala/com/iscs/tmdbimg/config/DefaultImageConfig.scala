package com.iscs.tmdbimg.config

import cats.effect.{Resource, Sync}
import java.io.InputStream

class DefaultImageConfig[F[_]: Sync](path: String) {
  private def resStream: InputStream = getClass.getResourceAsStream(path)

  private def getBytes(is: InputStream): F[Array[Byte]] = Sync[F].delay {
    if (is == null)  throw new IllegalArgumentException(s"Resource not found: $path")
    else is.readAllBytes
  }

  def getResource: Resource[F, Array[Byte]] = Resource.fromAutoCloseable {
    Sync[F].delay(resStream)
  }.evalMap(getBytes)
}
