package com.iscs.tmdbimg.api

sealed trait PATHTYPE
case object FIND   extends PATHTYPE
case object POSTER extends PATHTYPE

final case class TMDBApiUri(base: String, imdbId: Option[String], imagePath: Option[String])

object TMDBApiUri {
  val findBase                        = "https://api.themoviedb.org"
  val posterBase                      = "https://image.tmdb.org"
  val findPath                        = "/3/find"
  val getPoster                       = "/t/p"
  val keyParam                        = "api_key"
  val keyValue: String                = sys.env.getOrElse("TMDBKEY", "NOKEY")
  val findParams: Map[String, String] = Map("language" -> "en-US", "external_source" -> "imdb_id")
  val smallWidth                      = "w200"
  val bigWidth                        = "w500"
  val keyField: String                = s"$keyParam=$keyValue"
  val reqdParams: String = findParams
    .map { case (k, v) =>
      s"$k=$v"
    }
    .mkString("&", "&", "")

  def apply(searchType: PATHTYPE, queryVal: String): TMDBApiUri =
    searchType match {
      case FIND   => TMDBApiUri(findBase, Some(queryVal), None)
      case POSTER => TMDBApiUri(posterBase, None, Some(queryVal))
    }

  private def posterSize(size: Option[String]): String = size match {
    case Some("B") => bigWidth
    case Some("S") => smallWidth
    case _         => smallWidth
  }

  def builder(tmdb: TMDBApiUri, size: Option[String] = None): String = (tmdb.imdbId, tmdb.imagePath) match {
    case (Some(id), None)   => s"${tmdb.base}$findPath/$id?$keyField$reqdParams"
    case (None, Some(path)) => s"${tmdb.base}$getPoster/${posterSize(size)}$path?$keyField"
    case _                  => ""
  }

}
