# EchoReader

EchoReader is an AI-powered reading companion. Read your books or articles aloud (or type them in), and EchoReader will act as a study mentor, analyzing your text in real-time to generate structured summaries, key insights, study flashcards, and profound reflections.

## Features
* **Real-time Voice Transcription:** Read aloud, and the app will capture and process your text.
* **AI Insights:** Uses the Groq API for lightning-fast analysis, providing summaries, key points, broad connections, and vocabulary.
* **Study Flashcards:** Automatically generates study questions to test your knowledge.
* **Master Notes:** Accumulates progress and notes across your entire reading session.

## Setup
To use the AI features, you will need a [Groq API Key](https://console.groq.com/keys). Open the app, navigate to Settings, and enter your API key.

## Getting the APK

### 1. Download via GitHub Actions (Easiest)
This repository is configured with a GitHub Actions workflow.
1. Fork or push to this repository.
2. Go to the **Actions** tab in your GitHub repository.
3. Click on the latest run of the "Build Android APK" workflow.
4. Scroll down to the **Artifacts** section and download the `app-debug.apk` zip file.

### 2. Build via GitHub Codespaces
This repository includes a `.devcontainer` configuration, making it ready for GitHub Codespaces.
1. On the GitHub repository page, click the **Code** button.
2. Switch to the **Codespaces** tab and click **Create codespace on main**.
3. Wait for the environment to build (it comes pre-installed with Java 17 and Android SDK).
4. Once the terminal is available, run the following command:
   ```bash
   ./gradlew assembleDebug
   ```
5. After the build completes, navigate in the file explorer to `app/build/outputs/apk/debug/`. Right-click `app-debug.apk` and select **Download**.

### 3. Local Development (Android Studio)
1. Clone the repository.
2. Open the project in Android Studio.
3. Let Gradle sync and build the project.
4. Click **Run** or use `./gradlew assembleDebug` from the terminal.
