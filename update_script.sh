sed -i '/repository.updateCard(card.copy(isMastered = !card.isMastered))/a \
            if (!card.isMastered) {\n                com.example.viewmodel.incrementMasteredCards(repository)\n                com.example.viewmodel.awardXp(repository, 20, "Mastered Card")\n            }' app/src/main/java/com/example/viewmodel/EchoReaderViewModel.kt
