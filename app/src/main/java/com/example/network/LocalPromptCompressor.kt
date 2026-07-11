package com.example.network

object LocalPromptCompressor {

    private val STOP_WORDS = setOf(
        "a", "an", "and", "are", "as", "at", "be", "by", "for", "from",
        "has", "he", "in", "is", "it", "its", "of", "on", "that", "the",
        "to", "was", "were", "will", "with", "this", "but", "they", "we", "you",
        "or", "which", "one", "would", "all", "there", "their", "so", "up", "out",
        "if", "about", "who", "get", "which", "go", "me", "when", "make", "can",
        "like", "time", "no", "just", "him", "know", "take", "people", "into", "year",
        "your", "good", "some", "could", "them", "see", "other", "than", "then", "now",
        "look", "only", "come", "its", "over", "think", "also", "back", "after", "use",
        "two", "how", "our", "work", "first", "well", "way", "even", "new", "want"
    )

    fun compress(text: String): String {
        if (text.isBlank()) return text
        
        var compressed = text

        // 1. Remove URLs
        compressed = compressed.replace(Regex("https?://\\S+"), "")

        // 2. Remove markdown/html artifacts if any
        compressed = compressed.replace(Regex("<[^>]*>"), "")

        // 3. Remove conversational filler phrases
        val fillers = listOf(
            "please note that", "as a matter of fact", "in order to",
            "due to the fact that", "for the purpose of", "with respect to"
        )
        for (filler in fillers) {
            compressed = compressed.replace(Regex("(?i)\\b$filler\\b"), "")
        }

        // 4. Remove stop words
        val words = compressed.split(Regex("\\s+"))
        val filteredWords = words.filter { word ->
            val cleanWord = word.replace(Regex("[^a-zA-Z]"), "").lowercase()
            !STOP_WORDS.contains(cleanWord)
        }
        compressed = filteredWords.joinToString(" ")

        // 5. Replace common long words with abbreviations
        val abbreviations = mapOf(
            "because" to "cuz",
            "especially" to "esp",
            "information" to "info",
            "example" to "ex",
            "approximately" to "approx",
            "between" to "btw",
            "important" to "impt",
            "something" to "sth",
            "someone" to "sb",
            "without" to "w/o",
            "development" to "dev",
            "through" to "thru",
            "though" to "tho",
            "although" to "altho",
            "technology" to "tech",
            "application" to "app",
            "introduction" to "intro"
        )
        for ((word, abbr) in abbreviations) {
            compressed = compressed.replace(Regex("(?i)\\b$word\\b"), abbr)
        }

        // 6. Condense spaces and newlines
        compressed = compressed.replace(Regex("\\s+"), " ").trim()

        return compressed
    }
}
