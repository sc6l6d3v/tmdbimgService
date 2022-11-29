package com.iscs.tmdbimg.api

final case class GeoIPApiUri(base: String, path: String, ip: String = "127.0.0.1")

object GeoIPApiUri {
  val base = "https://api.ipgeolocation.io"
  val path = "/ipgeo"
  val keyParam = "apiKey"
  val keyValue: String = sys.env.getOrElse("TMDBKEY", "NOKEY")
  val fieldsParam = "fields"
  val fieldsValues = "geo,organization"

  def apply(ip: String): GeoIPApiUri = {
    GeoIPApiUri(base, path, ip)
  }

  def builder(uri: GeoIPApiUri): String = s"${uri.base}${uri.path}?$keyParam=$keyValue&ip=${uri.ip}&$fieldsParam=$fieldsValues"
}
