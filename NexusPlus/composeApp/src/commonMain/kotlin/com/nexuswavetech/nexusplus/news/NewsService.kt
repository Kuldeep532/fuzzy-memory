package com.nexuswavetech.nexusplus.news

import com.nexuswavetech.nexusplus.platform.fetchHttp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable

/**
 * NewsService — RSS feeds with categories, search, bookmark, offline cache, share, read aloud.
 *
 * Priority 6: News & ePapers.
 */
@Serializable
data class NewsArticle(
    val id: String,
    val title: String,
    val description: String,
    val url: String,
    val category: String,
    val publishedAt: String,
    val source: String,
    val isBookmarked: Boolean = false,
    val isOffline: Boolean = false,
    val content: String? = null,
)

/** Pre-configured RSS feeds for major categories. */
object NewsFeedRegistry {
    val feeds = mapOf(
        "World" to "https://feeds.bbci.co.uk/news/world/rss.xml",
        "Technology" to "https://feeds.bbci.co.uk/news/technology/rss.xml",
        "Science" to "https://feeds.bbci.co.uk/news/science_and_environment/rss.xml",
        "Business" to "https://feeds.bbci.co.uk/news/business/rss.xml",
        "Health" to "https://feeds.bbci.co.uk/news/health/rss.xml",
        "Sports" to "https://feeds.bbci.co.uk/sport/rss.xml",
        "Entertainment" to "https://feeds.bbci.co.uk/news/entertainment_and_arts/rss.xml",
        "India" to "https://feeds.bbci.co.uk/news/world/asia/india/rss.xml",
    )

    val categories: List<String> = feeds.keys.toList()
}

/**
 * NewsService fetches RSS feeds, parses them, and provides search/filter.
 * Uses a simple XML-to-JSON approach or direct RSS parsing.
 */
class NewsService {

    /** Fetch articles from a given category feed. */
    suspend fun fetchCategory(category: String): Result<List<NewsArticle>> =
        withContext(Dispatchers.IO) {
            try {
                val url = NewsFeedRegistry.feeds[category]
                    ?: return@withContext Result.failure(Exception("Unknown category: $category"))
                val rssXml = fetchHttp(url)
                val articles = parseRss(rssXml, category)
                Result.success(articles)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    /** Search across all categories (fetches all, then filters). */
    suspend fun search(query: String): Result<List<NewsArticle>> =
        withContext(Dispatchers.IO) {
            try {
                val all = mutableListOf<NewsArticle>()
                NewsFeedRegistry.feeds.forEach { (cat, url) ->
                    try {
                        val rssXml = fetchHttp(url)
                        all += parseRss(rssXml, cat)
                    } catch (_: Exception) { /* skip failed feeds */ }
                }
                val q = query.lowercase()
                val filtered = all.filter {
                    it.title.lowercase().contains(q) || it.description.lowercase().contains(q)
                }
                Result.success(filtered)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    private fun parseRss(xml: String, category: String): List<NewsArticle> {
        val items = mutableListOf<NewsArticle>()
        val regex = Regex("<item>(.*?)</item>", RegexOption.DOT_MATCHES_ALL)
        val titleRegex = Regex("<title>(.*?)</title>", RegexOption.DOT_MATCHES_ALL)
        val descRegex = Regex("<description>(.*?)</description>", RegexOption.DOT_MATCHES_ALL)
        val linkRegex = Regex("<link>(.*?)</link>", RegexOption.DOT_MATCHES_ALL)
        val pubRegex = Regex("<pubDate>(.*?)</pubDate>", RegexOption.DOT_MATCHES_ALL)

        regex.findAll(xml).forEachIndexed { index, match ->
            val item = match.groupValues[1]
            val title = titleRegex.find(item)?.groupValues?.get(1)?.trim() ?: ""
            val desc = descRegex.find(item)?.groupValues?.get(1)?.trim() ?: ""
            val link = linkRegex.find(item)?.groupValues?.get(1)?.trim() ?: ""
            val pub = pubRegex.find(item)?.groupValues?.get(1)?.trim() ?: ""
            items += NewsArticle(
                id = "${category}_$index",
                title = title,
                description = desc,
                url = link,
                category = category,
                publishedAt = pub,
                source = "BBC",
            )
        }
        return items
    }
}
