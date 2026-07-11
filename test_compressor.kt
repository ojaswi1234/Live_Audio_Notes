import java.util.regex.Pattern

fun compress(text: String): String {
    val STOP_WORDS = setOf(
        "a", "an", "and", "are", "as", "at", "be", "by", "for", "from",
        "has", "he", "in", "is", "it", "its", "of", "on", "that", "the",
        "to", "was", "were", "will", "with", "this", "but", "they", "we", "you",
        "or", "which", "one", "would", "all", "there", "their", "so", "up", "out"
    )

    if (text.isBlank()) return text
    
    var compressed = text

    // 1. Remove URLs
    compressed = compressed.replace(Regex("https?://\\S+"), "")

    // 2. Remove markdown/html artifacts if any
    compressed = compressed.replace(Regex("<[^>]*>"), "")

    // 3. Remove stop words
    val words = compressed.split(Regex("\\s+"))
    val filteredWords = words.filter { word ->
        val cleanWord = word.replace(Regex("[^a-zA-Z]"), "").lowercase()
        !STOP_WORDS.contains(cleanWord)
    }
    compressed = filteredWords.joinToString(" ")

    // 4. Replace common long words with abbreviations
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
        "development" to "dev"
    )
    for ((word, abbr) in abbreviations) {
        compressed = compressed.replace(Regex("(?i)\\b$word\\b"), abbr)
    }

    // 5. Condense spaces and newlines
    compressed = compressed.replace(Regex("\\s+"), " ").trim()

    return compressed
}

fun main() {
    val input = "The quick brown fox jumps over the lazy dog. He was looking for some food, but all he found was a piece of information."
    println("Original (${input.length}): $input")
    val result = compress(input)
    println("Compressed (${result.length}): $result")
}
