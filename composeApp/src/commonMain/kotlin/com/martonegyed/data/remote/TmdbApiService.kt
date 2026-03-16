package com.martonegyed.data.remote

import com.martonegyed.BuildKonfig
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class TmdbSearchResponse(
    val results: List<TmdbMovie>
)

@Serializable
data class TmdbFindResponse(
    @SerialName("movie_results") val movieResults: List<TmdbMovie>
)

@Serializable
data class TmdbMovie(
    val id: Int,
    val title: String,
    @SerialName("poster_path") val posterPath: String? = null,
    @SerialName("backdrop_path") val backdropPath: String? = null,
    val overview: String? = null,
    @SerialName("release_date") val releaseDate: String? = null
)

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

    suspend fun getMovieDetails(tmdbId: Int): JsonObject? {
        return try {
            client.get("$baseUrl/movie/$tmdbId") {
                parameter("api_key", apiKey)
                parameter("append_to_response", "credits,similar,reviews")
            }.body()
        } catch (e: Exception) {
            println("TMDB Inquiry Error: ${e.message}")
            null
        }
    }
}