package com.iscs.tmdbimg.api

import org.specs2.matcher.MatchResult
import org.specs2.mutable.Specification

class TMDBImgApiUriSpec extends Specification {

  private val base = "https://api.themoviedb.org"
  private val path = "/3/find/"
  private val keyParam = "api_key"
  private val keyValue = sys.env.getOrElse("TMDBKEY", "NOKEY")
  private def fullBase(maybeKey: Option[String]) = maybeKey match {
    case Some(key) => s"$base$path$key?$keyParam=$keyValue"
    case _         => s"$base$path?$keyParam=$keyValue"
  }
  private val fieldMap = Map("language" -> "en-US", "external_source" -> "imdb_id")
  private val fields = fieldMap.map{ case (k,v) => s"$k=$v"}.mkString("&")

  private[this] def baseCheck(): MatchResult[String] =
    TMDBApiUri.builder(TMDBApiUri(FIND, "")) must beEqualTo(s"${fullBase(Some(""))}&$fields")

  private[this] def withIMDBId(): MatchResult[String] =
    TMDBApiUri.builder(TMDBApiUri(FIND, "tt104343")) must beEqualTo(s"${fullBase(Some("tt104343"))}&$fields")

  "TMDBApiUri" >> {
    "base checks" >> {
      baseCheck()
    }
    "withQuery override" >> {
      withIMDBId()
    }
  }

}
