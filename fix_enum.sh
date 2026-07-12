sed -i '/enum class AppScreen {/,/class EchoReaderViewModel/c\
enum class AppScreen {\
    API_SETUP_INSTRUCTIONS,\
    API_SETUP_INPUT,\
    API_KEY_MANAGER,\
    ONBOARDING,\
    GOALS_SETUP,\
    SESSION_DASHBOARD,\
    HISTORY_LIST,\
    STUDY_QUIZ,\
    PROFILE_REWARDS,\
    BOOK_CLUB,\
    LEADERBOARD\
}\
\
class EchoReaderViewModel(application: Application) : AndroidViewModel(application), TextToSpeech.OnInitListener {' app/src/main/java/com/example/viewmodel/EchoReaderViewModel.kt
