import sys

with open('app/src/main/java/com/example/viewmodel/EchoReaderViewModel.kt', 'r') as f:
    text = f.read()

target1 = """    private val _currentScreen = MutableStateFlow(
        if (com.example.network.FirebaseManager.auth.currentUser == null) AppScreen.AUTH
        else if (prefs.getString("groq_api_key", "").isNullOrEmpty() && com.example.BuildConfig.GROQ_API_KEY == "MY_GROQ_API_KEY") AppScreen.API_SETUP_INSTRUCTIONS 
        else AppScreen.ONBOARDING
    )"""

replacement1 = """    private val _currentScreen = MutableStateFlow(
        if (com.example.network.FirebaseManager.auth.currentUser == null) AppScreen.AUTH
        else if (prefs.getString("groq_api_key", "").isNullOrEmpty() && com.example.BuildConfig.GROQ_API_KEY == "MY_GROQ_API_KEY") AppScreen.API_SETUP_INSTRUCTIONS 
        else AppScreen.HISTORY_LIST
    )"""

target2 = """            com.example.network.FirebaseManager.saveUserProfile(name, age, interests, favouriteBooks, profilePicUri)
            navigateTo(AppScreen.ONBOARDING)
        }
    }"""

replacement2 = """            com.example.network.FirebaseManager.saveUserProfile(name, age, interests, favouriteBooks, profilePicUri)
            navigateTo(AppScreen.HISTORY_LIST)
        }
    }"""

target3 = """    ONBOARDING,
    GOALS_SETUP,"""

replacement3 = """    GOALS_SETUP,"""

text = text.replace(target1, replacement1)
text = text.replace(target2, replacement2)
text = text.replace(target3, replacement3)

with open('app/src/main/java/com/example/viewmodel/EchoReaderViewModel.kt', 'w') as f:
    f.write(text)
