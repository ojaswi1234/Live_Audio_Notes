import re

content = """
    suspend fun getLeaderboard(): List<Pair<String, Int>> = withContext(Dispatchers.IO) {
        val proxyUrl = "$PROXY_URL/api/leaderboard"
        
        val request = Request.Builder()
            .url(proxyUrl)
            .get()
            .build()
            
        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext getMockLeaderboard()
                }
                val responseBody = response.body?.string() ?: return@withContext getMockLeaderboard()
                val json = JSONObject(responseBody)
                val boardArray = json.optJSONArray("leaderboard")
                val result = mutableListOf<Pair<String, Int>>()
                if (boardArray != null) {
                    for (i in 0 until boardArray.length()) {
                        val obj = boardArray.optJSONObject(i)
                        if (obj != null) {
                            result.add(Pair(obj.optString("username", "Unknown"), obj.optInt("level", 1)))
                        }
                    }
                }
                if (result.isEmpty()) getMockLeaderboard() else result
            }
        } catch (e: Exception) {
            getMockLeaderboard()
        }
    }
    
    suspend fun updateLeaderboard(username: String, level: Int, xp: Int) = withContext(Dispatchers.IO) {
        val proxyUrl = "$PROXY_URL/api/leaderboard"
        val requestBodyJson = JSONObject().apply {
            put("username", username)
            put("level", level)
            put("xp", xp)
        }
        val requestBody = requestBodyJson.toString().toRequestBody(JSON_MEDIA_TYPE)
        val request = Request.Builder()
            .url(proxyUrl)
            .post(requestBody)
            .build()
            
        try {
            client.newCall(request).execute().close()
        } catch (e: Exception) {
            // Ignore error if backend isn't deployed yet
        }
    }
    
    private fun getMockLeaderboard(): List<Pair<String, Int>> {
        return listOf(
            Pair("AlexReads", 12),
            Pair("BookWorm99", 10),
            Pair("LiteratureLover", 8),
            Pair("StudyMaster", 5),
            Pair("NoviceReader", 2)
        )
    }
"""

with open('app/src/main/java/com/example/network/GroqClient.kt', 'r') as f:
    text = f.read()

# Insert before the last brace
text = text.rsplit('}', 1)[0] + content + "\n}"

with open('app/src/main/java/com/example/network/GroqClient.kt', 'w') as f:
    f.write(text)
