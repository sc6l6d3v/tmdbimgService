package com.iscs.tmdbimg.domains

import com.iscs.tmdbimg.model.*
import com.iscs.tmdbimg.model.MediaTypes.keyMapping
import zio.*
import zio.json.*
import zio.test.*
import zio.test.Assertion.*

object TMDBImgSpec extends ZIOSpecDefault {

  private val knownFor = TMDBPersonKnownFor(
    adult             = false,
    backdrop_path     = Some("/knownfor.jpg"),
    id                = 99,
    title             = "Known Movie",
    original_language = "en",
    original_title    = "Known Movie",
    overview          = "Known for this",
    poster_path       = Some("/poster.jpg"),
    media_type        = "movie",
    genre_ids         = List(18),
    popularity        = 5.0,
    release_date      = "2020-06-01",
    video             = false,
    vote_average      = 6.5,
    vote_count        = 800
  )

  private val movieFixture = MovieResults(
    adult             = false,
    backdrop_path     = Some("/backdrop.jpg"),
    id                = 12345,
    title             = "Test Movie",
    original_language = "en",
    original_title    = "Test Movie",
    overview          = "A test movie",
    poster_path       = Some("/poster.jpg"),
    media_type        = "movie",
    genre_ids         = List(28, 12),
    popularity        = 7.5,
    release_date      = "2023-01-15",
    video             = false,
    vote_average      = 7.2,
    vote_count        = 1500
  )

  private val tvFixture = TVResults(
    adult             = false,
    backdrop_path     = Some("/tv_backdrop.jpg"),
    id                = 67890,
    name              = "Test Series",
    original_language = "en",
    original_name     = "Test Series",
    overview          = "A test series",
    poster_path       = Some("/tv_poster.jpg"),
    media_type        = "tv",
    genre_ids         = List(18, 9648),
    popularity        = 6.3,
    first_air_date    = "2021-03-10",
    vote_average      = 8.1,
    vote_count        = 3200,
    origin_country    = List("US")
  )

  private val episodeFixture = EpisodeResults(
    id              = 11111,
    name            = "Pilot",
    overview        = "The first episode",
    media_type      = "tv_episode",
    vote_average    = 7.8,
    vote_count      = 420,
    air_date        = "2021-03-10",
    episode_number  = 1,
    episode_type    = "standard",
    production_code = "",
    runtime         = Some(45),
    season_number   = 1,
    show_id         = 67890,
    still_path      = Some("/still.jpg")
  )

  // Typed as Meta because PersonResults has no companion encoder;
  // tmdbMetaEncoder (the sum type encoder) handles serialization.
  private val personFixture: Meta = PersonResults(
    adult                = false,
    id                   = 54321,
    name                 = "Test Person",
    original_name        = "Test Person",
    media_type           = "person",
    popularity           = 3.2,
    gender               = 1,
    known_for_department = "Acting",
    profile_path         = "/profile.jpg",
    known_for            = List(knownFor)
  )

  private def applyKeyMapping(json: String): String =
    keyMapping.foldLeft(json) { case (s, (k, v)) =>
      s.replaceAll(s""""$k":\\[\\{""", s""""$k":\\[{"type":"$v",""")
    }

  private def wrapResult(key: String, itemJson: String): String = {
    val keys = List("movie_results", "person_results", "tv_results", "tv_episode_results", "tv_season_results")
    keys.map(k => s""""$k":${if (k == key) s"[$itemJson]" else "[]"}""").mkString("{", ",", "}")
  }

  val spec: Spec[Environment, Any] =
    suite("TMDBImgSpec")(

      // Validates the Redis read/write cycle: checkSetMeta stores tmdb.toJson,
      // getMetaFromRedis decodes it back via fromMeta.
      suite("fromMeta round-trip")(
        test("MovieResults survives encode/decode cycle") {
          val meta: Meta = movieFixture
          assert(TMDBImg.fromMeta(meta.toJson))(isSome(equalTo(meta)))
        },
        test("TVResults survives encode/decode cycle") {
          val meta: Meta = tvFixture
          assert(TMDBImg.fromMeta(meta.toJson))(isSome(equalTo(meta)))
        },
        test("EpisodeResults survives encode/decode cycle") {
          val meta: Meta = episodeFixture
          assert(TMDBImg.fromMeta(meta.toJson))(isSome(equalTo(meta)))
        },
        test("PersonResults survives encode/decode cycle") {
          assert(TMDBImg.fromMeta(personFixture.toJson))(isSome(equalTo(personFixture)))
        },
        test("returns None on malformed JSON") {
          assert(TMDBImg.fromMeta("""{"garbage":true}"""))(isNone)
        }
      ),

      // Validates the regex substitution in getMetaData that injects "type"
      // before decoding raw TMDB API responses (which carry no discriminator).
      suite("keyMapping transform")(
        test("injects type discriminator into populated movie_results") {
          assert(applyKeyMapping("""{"movie_results":[{"id":1}]}"""))(
            containsString(""""type":"MovieResults"""")
          )
        },
        test("injects type discriminator into populated tv_results") {
          assert(applyKeyMapping("""{"tv_results":[{"id":1}]}"""))(
            containsString(""""type":"TVResults"""")
          )
        },
        test("injects type discriminator into populated tv_episode_results") {
          assert(applyKeyMapping("""{"tv_episode_results":[{"id":1}]}"""))(
            containsString(""""type":"EpisodeResults"""")
          )
        },
        test("does not modify empty result arrays") {
          assert(applyKeyMapping("""{"movie_results":[],"tv_results":[]}"""))(
            not(containsString("type"))
          )
        }
      ),

      // Validates the full getMetaData pipeline: raw TMDB JSON (no type field)
      // → keyMapping inject → MediaTypes decode → correct subtype in result list.
      // PersonResults is excluded: no product encoder exists in its companion.
      suite("MediaTypes pipeline")(
        test("decodes MovieResults from raw TMDB response") {
          val rawItem = MovieResults.movieResultsEncoder.encodeJson(movieFixture, None).toString
          val decoded = applyKeyMapping(wrapResult("movie_results", rawItem)).fromJson[MediaTypes]
          assert(decoded.map(_.movie_results.headOption))(isRight(isSome(equalTo(movieFixture: Meta))))
        },
        test("decodes TVResults from raw TMDB response") {
          val rawItem = TVResults.tvResultsEncoder.encodeJson(tvFixture, None).toString
          val decoded = applyKeyMapping(wrapResult("tv_results", rawItem)).fromJson[MediaTypes]
          assert(decoded.map(_.tv_results.headOption))(isRight(isSome(equalTo(tvFixture: Meta))))
        },
        test("decodes EpisodeResults from raw TMDB response") {
          val rawItem = EpisodeResults.episodeResultsEncoder.encodeJson(episodeFixture, None).toString
          val decoded = applyKeyMapping(wrapResult("tv_episode_results", rawItem)).fromJson[MediaTypes]
          assert(decoded.map(_.tv_episode_results.headOption))(isRight(isSome(equalTo(episodeFixture: Meta))))
        },
        test("all result lists empty when TMDB returns no matches") {
          val empty = """{"movie_results":[],"person_results":[],"tv_results":[],"tv_episode_results":[],"tv_season_results":[]}"""
          val allEmpty = empty.fromJson[MediaTypes].map(mt =>
            List(mt.movie_results, mt.person_results, mt.tv_results, mt.tv_episode_results, mt.tv_season_results).forall(_.isEmpty)
          )
          assert(allEmpty)(isRight(isTrue))
        }
      )
    )
}
