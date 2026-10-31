package com.pagewise.app.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/** A search hit from an online catalogue, not yet saved to the library. */
data class BookCandidate(
    val title: String,
    val authors: String,
    val isbn: String?,
    val coverUrl: String?,
    val pageCount: Int,
    val description: String?,
    val publisher: String?,
)

data class Definition(val word: String, val partOfSpeech: String, val meaning: String)

class RemoteSource {
    private val http = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()
    private val json = Json { ignoreUnknownKeys = true }

    private suspend fun getJson(url: okhttp3.HttpUrl): JsonElement? = withContext(Dispatchers.IO) {
        http.newCall(Request.Builder().url(url).build()).execute().use { resp ->
            if (!resp.isSuccessful) return@withContext null
            resp.body?.string()?.let { json.parseToJsonElement(it) }
        }
    }

    /** Searches Google Books; without an API key it is often rate-limited, so Open Library is the fallback. */
    suspend fun searchBooks(query: String): List<BookCandidate> =
        runCatching { searchGoogle(query) }.getOrDefault(emptyList()).ifEmpty { searchOpenLibrary(query) }

    private suspend fun searchGoogle(query: String): List<BookCandidate> {
        val url = "https://www.googleapis.com/books/v1/volumes".toHttpUrl().newBuilder()
            .addQueryParameter("q", query)
            .addQueryParameter("maxResults", "30")
            .addQueryParameter("printType", "books")
            .build()
        val items = getJson(url)?.jsonObject?.get("items") as? JsonArray ?: return emptyList()
        return items.mapNotNull { parseGoogleVolume(it.jsonObject) }
    }

    private suspend fun searchOpenLibrary(query: String): List<BookCandidate> {
        val q = query.removePrefix("intitle:")
        val builder = "https://openlibrary.org/search.json".toHttpUrl().newBuilder()
            .addQueryParameter("limit", "30")
            .addQueryParameter("fields", "title,author_name,isbn,cover_i,number_of_pages_median,publisher")
        if (q.startsWith("isbn:")) builder.addQueryParameter("isbn", q.removePrefix("isbn:")) else builder.addQueryParameter("q", q)
        val docs = getJson(builder.build())?.jsonObject?.get("docs") as? JsonArray ?: return emptyList()
        return docs.mapNotNull { d ->
            val o = d.jsonObject
            BookCandidate(
                title = o.str("title") ?: return@mapNotNull null,
                authors = (o["author_name"] as? JsonArray)?.take(3)?.joinToString { it.jsonPrimitive.content }.orEmpty(),
                isbn = (o["isbn"] as? JsonArray)?.map { it.jsonPrimitive.content }?.let { list -> list.firstOrNull { it.length == 13 } ?: list.firstOrNull() },
                coverUrl = o["cover_i"]?.jsonPrimitive?.intOrNull?.let { "https://covers.openlibrary.org/b/id/$it-M.jpg" },
                pageCount = o["number_of_pages_median"]?.jsonPrimitive?.intOrNull ?: 0,
                description = null,
                publisher = (o["publisher"] as? JsonArray)?.firstOrNull()?.jsonPrimitive?.content,
            )
        }
    }

    /** Looks an ISBN up on Google Books, falling back to Open Library. */
    suspend fun lookupIsbn(isbn: String): BookCandidate? {
        val clean = isbn.filter { it.isDigit() || it == 'X' || it == 'x' }
        searchBooks("isbn:$clean").firstOrNull()?.let { return it.copy(isbn = clean) }
        val url = "https://openlibrary.org/api/books".toHttpUrl().newBuilder()
            .addQueryParameter("bibkeys", "ISBN:$clean")
            .addQueryParameter("format", "json")
            .addQueryParameter("jscmd", "data")
            .build()
        val obj = getJson(url)?.jsonObject?.get("ISBN:$clean")?.jsonObject ?: return null
        return BookCandidate(
            title = obj.str("title") ?: return null,
            authors = (obj["authors"] as? JsonArray)?.joinToString { it.jsonObject.str("name").orEmpty() }.orEmpty(),
            isbn = clean,
            coverUrl = (obj["cover"] as? JsonObject)?.str("large")?.replace("http://", "https://"),
            pageCount = obj["number_of_pages"]?.jsonPrimitive?.intOrNull ?: 0,
            description = null,
            publisher = (obj["publishers"] as? JsonArray)?.firstOrNull()?.jsonObject?.str("name"),
        )
    }

    private fun parseGoogleVolume(item: JsonObject): BookCandidate? {
        val info = item["volumeInfo"]?.jsonObject ?: return null
        val ids = (info["industryIdentifiers"] as? JsonArray)?.map { it.jsonObject }.orEmpty()
        val isbn = ids.firstOrNull { it.str("type") == "ISBN_13" }?.str("identifier")
            ?: ids.firstOrNull { it.str("type") == "ISBN_10" }?.str("identifier")
        val images = info["imageLinks"] as? JsonObject
        val cover = (images?.str("thumbnail") ?: images?.str("smallThumbnail"))
            ?.replace("http://", "https://")
            ?.replace("&edge=curl", "")
        return BookCandidate(
            title = info.str("title") ?: return null,
            authors = (info["authors"] as? JsonArray)?.joinToString { it.jsonPrimitive.content }.orEmpty(),
            isbn = isbn,
            coverUrl = cover,
            pageCount = info["pageCount"]?.jsonPrimitive?.intOrNull ?: 0,
            description = info.str("description"),
            publisher = info.str("publisher"),
        )
    }

    /** Cover image URLs for a title, used by the cover picker. */
    suspend fun searchCovers(title: String, authors: String): List<String> =
        searchBooks("intitle:$title ${authors.split(",").firstOrNull().orEmpty()}")
            .mapNotNull { it.coverUrl }
            .distinct()

    suspend fun define(word: String): List<Definition> {
        val url = "https://api.dictionaryapi.dev/api/v2/entries/en/".toHttpUrl().newBuilder()
            .addPathSegment(word.trim().lowercase())
            .build()
        val entries = runCatching { getJson(url) }.getOrNull() as? JsonArray ?: return emptyList()
        return entries.flatMap { entry ->
            entry.jsonObject["meanings"]?.jsonArray.orEmpty().flatMap { m ->
                val pos = m.jsonObject.str("partOfSpeech").orEmpty()
                m.jsonObject["definitions"]?.jsonArray.orEmpty().take(2).mapNotNull { d ->
                    d.jsonObject.str("definition")?.let { Definition(word, pos, it) }
                }
            }
        }.take(6)
    }

    private fun JsonObject.str(key: String): String? = (this[key] as? kotlinx.serialization.json.JsonPrimitive)?.contentOrNull
}
