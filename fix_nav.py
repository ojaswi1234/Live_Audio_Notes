import sys

with open('app/src/main/java/com/example/viewmodel/EchoReaderViewModel.kt', 'r') as f:
    text = f.read()

target = """            com.example.network.FirebaseManager.saveUserProfile(name, age, interests, favouriteBooks, profilePicUri)
            navigateTo(AppScreen.USER_SETUP)"""
replacement = """            com.example.network.FirebaseManager.saveUserProfile(name, age, interests, favouriteBooks, profilePicUri)
            navigateTo(AppScreen.ONBOARDING)"""
text = text.replace(target, replacement)

with open('app/src/main/java/com/example/viewmodel/EchoReaderViewModel.kt', 'w') as f:
    f.write(text)
