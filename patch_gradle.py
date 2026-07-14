import sys

with open('app/build.gradle.kts', 'r') as f:
    text = f.read()

target = """  // Uncomment to use Firestore:
  // implementation(libs.firebase.firestore)
  // Firebase Auth with Google Sign-In requires all of the following to be uncommented together.
  // If you are using Firebase Auth with other providers (e.g. Email/Password), you may only need
  // firebase-auth.
  // implementation(libs.firebase.auth)
  // implementation(libs.androidx.credentials)
  // implementation(libs.androidx.credentials.play.services)
  // implementation(libs.googleid)"""

replacement = """  // Firebase additions
  implementation(libs.firebase.firestore)
  implementation(libs.firebase.auth)
  implementation(libs.androidx.credentials)
  implementation(libs.androidx.credentials.play.services)
  implementation(libs.googleid)
  implementation(libs.firebase.messaging)"""

text = text.replace(target, replacement)

with open('app/build.gradle.kts', 'w') as f:
    f.write(text)
