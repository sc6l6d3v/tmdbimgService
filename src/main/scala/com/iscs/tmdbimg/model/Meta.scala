package com.iscs.tmdbimg.model

import zio.json._

//@jsonDiscriminator("hint")
sealed trait Meta extends Product with Serializable
final case class TMDBPersonKnownFor(adult: Boolean, backdrop_path: String, id: Int, title: String, original_language: String,
                                    original_title: String, overview: String, poster_path: String, media_type: String,
                                    genre_ids: List[Int], popularity: Double, release_date: String, video: Boolean,
                                    vote_average: Double, vote_count: Int)

//@jsonHint("person")
case class PersonResults(adult: Boolean, id: Int, name: String, original_name: String,
                         media_type: String, popularity: Double, gender: Int, known_for_department: String,
                         profile_path: String, known_for: List[TMDBPersonKnownFor]) extends Meta

//@jsonHint("movie")
case class MovieResults(adult: Boolean, backdrop_path: String, id: Int, title: String, original_language: String,
                        original_title: String, overview: String, poster_path: String, media_type: String,
                        genre_ids: List[Int], popularity: Double, release_date: String, video: Boolean,
                        vote_average: Double, vote_count: Int) extends Meta

//@jsonHint("tv")
case class TVResults(adult: Boolean, backdrop_path: String, id: Int, name: String, original_language: String,
                     original_name: String, overview: String, poster_path: String, media_type: String,
                     genre_ids: List[Int], popularity: Double, first_air_date: String,
                     vote_average: Double, vote_count: Int, origin_country: List[String]) extends Meta

//@jsonHint("episode")
case class EpisodeResults(id: Int, name: String, overview: String, media_type: String,
                          vote_average: Double, vote_count: Int, air_date: String, episode_number: Int,
                          production_code: String, runtime: Int, season_number: Int, show_id: Int,
                          still_path: String) extends Meta

case class MediaTypes(movie_results: List[Meta] = List(),
                      person_results: List[Meta] = List(),
                      tv_results: List[Meta] = List(),
                      tv_episode_results: List[Meta] = List(),
                      tv_season_results: List[Meta] = List()
                     )

object TMDBPersonKnownFor {
  implicit val tmdbPersonKnownForDecoder: JsonDecoder[TMDBPersonKnownFor] = DeriveJsonDecoder.gen[TMDBPersonKnownFor]
  implicit val tmdbPersonKnownForEncoder: JsonEncoder[TMDBPersonKnownFor] = DeriveJsonEncoder.gen[TMDBPersonKnownFor]
}

object MediaTypes {
  val keyMapping = Map(
    "movie_results" -> "MovieResults",
    "person_results" -> "PersonResults",
    "tv_results" -> "TVResults",
    "tv_episode_results" -> "EpisodeResults",
    "tv_season_results" -> "EpisodeResults"
  )
  implicit val tmdbMediaTypesDecoder: JsonDecoder[MediaTypes] = DeriveJsonDecoder.gen[MediaTypes]
  implicit val tmdbMediaTypesEncoder: JsonEncoder[MediaTypes] = DeriveJsonEncoder.gen[MediaTypes]
}

object Meta {
  implicit val tmdbMetaDecoder: JsonDecoder[Meta] = DeriveJsonDecoder.gen[Meta]
  implicit val tmdbMetaEncoder: JsonEncoder[Meta] = DeriveJsonEncoder.gen[Meta]
}

