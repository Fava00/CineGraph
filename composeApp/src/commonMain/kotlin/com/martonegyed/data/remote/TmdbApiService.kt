package com.martonegyed.data.remote

import com.martonegyed.BuildKonfig
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TmdbMovie(
    val id: Int,
    val title: String,
    @SerialName("poster_path") val posterPath: String? = null,
    @SerialName("backdrop_path") val backdropPath: String? = null,
    val overview: String? = null,
    @SerialName("release_date") val releaseDate: String? = null,
    @SerialName("vote_average") val voteAverage: Double? = null,
    @SerialName("vote_count") val voteCount: Int? = null,
    @SerialName("original_language") val originalLanguage: String? = null,
    @SerialName("genre_ids") val genreIds: List<Int> = emptyList()
)

@Serializable
data class TmdbSearchResponse(
    val results: List<TmdbMovie>
)


@Serializable
data class TmdbFindResponse(
    @SerialName("movie_results") val movieResults: List<TmdbMovie>
)

@Serializable
data class TmdbGenre(val id: Int, val name: String)

@Serializable
data class TmdbProductionCompany(val name: String)

@Serializable
data class TmdbProductionCountry(
    @SerialName("name") val name: String
)

@Serializable
data class TmdbSpokenLanguage(
    @SerialName("english_name") val englishName: String
)

@Serializable
data class TmdbCollection(val name: String? = null)


@Serializable
data class TmdbCreditsResponse(
    val cast: List<TmdbCast> = emptyList(),
    val crew: List<TmdbCrew> = emptyList()
)

@Serializable
data class TmdbCast(
    val name: String,
    val character: String? = null,
    @SerialName("profile_path") val profilePath: String? = null
)

@Serializable
data class TmdbCrew(
    val name: String,
    val job: String,
    @SerialName("profile_path") val profilePath: String? = null
)


@Serializable
data class TmdbSimilarResponse(val results: List<TmdbMovie> = emptyList())


@Serializable
data class TmdbReviewsResponse(val results: List<TmdbReview> = emptyList())

@Serializable
data class TmdbReview(
    val author: String = "",
    val content: String = ""
)
@Serializable
data class TmdbVideosResponse(val results: List<TmdbVideo> = emptyList())

@Serializable
data class TmdbVideo(
    val key: String,
    val site: String,
    val type: String,
    val official: Boolean = false
)
@Serializable
data class TmdbReleaseDatesResponse(val results: List<TmdbReleaseDateCountry> = emptyList())

@Serializable
data class TmdbReleaseDateCountry(
    @SerialName("iso_3166_1") val country: String,
    @SerialName("release_dates") val releaseDates: List<TmdbReleaseDate> = emptyList()
)

@Serializable
data class TmdbReleaseDate(
    val certification: String = "",
    @SerialName("release_date") val releaseDate: String = ""
)

@Serializable
data class TmdbTranslationsResponse(val translations: List<TmdbTranslation> = emptyList())

@Serializable
data class TmdbTranslation(
    @SerialName("iso_3166_1") val country: String,
    @SerialName("iso_639_1") val language: String,
    val data: TmdbTranslationData = TmdbTranslationData()
)

@Serializable
data class TmdbTranslationData(val title: String = "")

@Serializable
data class TmdbMovieDetailsResponse(
    val id: Int = 0,
    @SerialName("poster_path") val posterPath: String? = null,
    @SerialName("backdrop_path") val backdropPath: String? = null,
    val overview: String? = null,
    val runtime: Int? = null,
    val tagline: String? = null,
    val budget: Long? = null,
    val revenue: Long? = null,
    @SerialName("original_title") val originalTitle: String? = null,
    @SerialName("original_language") val originalLanguage: String? = null,
    @SerialName("imdb_id") val imdbId: String? = null,
    val popularity: Double? = null,
    @SerialName("vote_average") val voteAverage: Double? = null,
    @SerialName("vote_count") val voteCount: Int? = null,
    @SerialName("belongs_to_collection") val collection: TmdbCollection? = null,
    val genres: List<TmdbGenre> = emptyList(),
    @SerialName("production_companies") val studios: List<TmdbProductionCompany> = emptyList(),
    @SerialName("production_countries") val productionCountries: List<TmdbProductionCountry> = emptyList(),
    @SerialName("spoken_languages") val spokenLanguages: List<TmdbSpokenLanguage> = emptyList(),
    val credits: TmdbCreditsResponse? = null,
    val similar: TmdbSimilarResponse? = null,
    val reviews: TmdbReviewsResponse? = null,
    val videos: TmdbVideosResponse? = null,
    @SerialName("release_dates") val releaseDates: TmdbReleaseDatesResponse? = null,
    val translations: TmdbTranslationsResponse? = null
) {
    val trailerKey: String?
        get() = videos?.results
            ?.filter { it.site == "YouTube" && it.type == "Trailer" }
            ?.maxByOrNull { if (it.official) 1 else 0 }
            ?.key
    val mpaaRating: String?
        get() = releaseDates?.results
            ?.firstOrNull { it.country == "US" }
            ?.releaseDates
            ?.firstOrNull { it.certification.isNotEmpty() }
            ?.certification

    val hungarianTitle: String?
        get() = translations?.translations
            ?.firstOrNull { it.country == "HU" && it.language == "hu" }
            ?.data?.title?.takeIf { it.isNotEmpty() }
}


class TmdbApiService(private val client: HttpClient) {

    private val apiKey = BuildKonfig.TMDB_API_KEY
    private val baseUrl = "https://api.themoviedb.org/3"

    suspend fun searchMovie(query: String, year: Int = 0): TmdbSearchResponse? {
        return try {
            client.get("https://api.themoviedb.org/3/search/movie") {
                parameter("api_key", BuildKonfig.TMDB_API_KEY)
                parameter("query", query)
                if (year > 0) parameter("primary_release_year", year)
            }.body()
        } catch (e: Exception) {
            println("TMDB Search Error: ${e.message}")
            null
        }
    }

    suspend fun findByImdbId(imdbId: String): TmdbFindResponse? {
        return try {
            client.get("https://api.themoviedb.org/3/find/$imdbId") {
                parameter("api_key", BuildKonfig.TMDB_API_KEY)
                parameter("external_source", "imdb_id")
            }.body()
        } catch (e: Exception) {
            println("TMDB Find Error: ${e.message}")
            null
        }
    }

    suspend fun getMovieDetails(tmdbId: Int): TmdbMovieDetailsResponse? {
        return try {
            client.get("$baseUrl/movie/$tmdbId") {
                parameter("api_key", apiKey)

                parameter("append_to_response", "credits,similar,reviews,videos,release_dates,translations")
            }.body()
        } catch (e: Exception) {
            println("TMDB Details Error: ${e.message}")
            null
        }
    }
}