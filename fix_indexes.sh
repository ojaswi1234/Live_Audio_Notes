sed -i 's/@Entity(tableName = "text_chunks")/@Entity(tableName = "text_chunks", indices = [androidx.room.Index(value = ["sessionId"])])/g' app/src/main/java/com/example/data/BookSession.kt
sed -i 's/@Entity(tableName = "study_cards")/@Entity(tableName = "study_cards", indices = [androidx.room.Index(value = ["sessionId"])])/g' app/src/main/java/com/example/data/BookSession.kt
