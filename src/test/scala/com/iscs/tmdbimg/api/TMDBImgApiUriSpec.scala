package com.iscs.tmdbimg.api

import org.specs2.matcher.MatchResult
import org.specs2.mutable.Specification

class TMDBImgApiUriSpec extends Specification {

  private val base = "https://api.ipgeolocation.io"
  private val path = "/ipgeo"
  private val keyParam = "apiKey"
  private val keyValue = sys.env.getOrElse("GEOIPKEY", "NOKEY")
  private val fullBase = s"$base$path?$keyParam=$keyValue"
  private val fieldsParam = "fields"
  private val fieldsValues = "geo,organization"

  private[this] def baseCheck(): MatchResult[String] =
    GeoIPApiUri.builder(GeoIPApiUri("")) must beEqualTo(s"$fullBase&ip=&$fieldsParam=$fieldsValues")

  private[this] def withIPCheck(): MatchResult[String] =
    GeoIPApiUri.builder(GeoIPApiUri("211.40.129.246")) must beEqualTo(s"$fullBase&ip=211.40.129.246&$fieldsParam=$fieldsValues")

  "GeoIPApiUri" >> {
    "base checks" >> {
      baseCheck()
    }
    "withQuery override" >> {
      withIPCheck()
    }
  }

}
