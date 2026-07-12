sed -i 's/val xpForNextLevel = level \* 100/val xpForNextLevel = com.example.viewmodel.getXpRequiredForLevelUp(level)/' app/src/main/java/com/example/ui/screens/ProfileScreen.kt
sed -i 's/val xpProgress = (xp % 100).toFloat() \/ 100f/val xpProgress = xp.toFloat() \/ xpForNextLevel.toFloat()/' app/src/main/java/com/example/ui/screens/ProfileScreen.kt
