sed -i '/private var tts: TextToSpeech? = null/a \
    val scienceFacts = listOf(\
        "When you speak while reading, both your speech and listening work in sync so that you can remember the information longer (known as the production effect).",\
        "Reading aloud forces you to slow down and process the material more deeply, improving overall comprehension and focus.",\
        "Hearing your own voice read the text aloud creates a distinct auditory memory trace, making it easier to recall later.",\
        "Combining visual and auditory processing through reading aloud engages multiple brain regions, strengthening neural pathways.",\
        "Active reading and vocalization can significantly reduce mind-wandering, keeping you anchored to the text.",\
        "Self-explanation and summarizing aloud helps you identify gaps in your understanding in real-time.",\
        "The physical act of articulating words aloud increases engagement and enhances cognitive function.",\
        "Dual-coding theory suggests that forming both visual and auditory memories of text significantly increases long-term retention."\
    )\
\
    var currentScienceFact by mutableStateOf(scienceFacts.random())\
        private set\
\
    var showScienceFactPopup by mutableStateOf(true)\
        private set\
\
    fun dismissScienceFactPopup() {\
        showScienceFactPopup = false\
    }' app/src/main/java/com/example/viewmodel/EchoReaderViewModel.kt
