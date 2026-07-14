<div align="center">

# 📖 EchoReader — Live Audio Notes

**An Android app that listens to you read aloud (or accepts pasted text) and turns it into structured, AI‑generated notes, flashcards, and quizzes — in real time.**

Built with Kotlin, Jetpack Compose, Room, and Groq's LLM API.

</div>

---

## What is this?

EchoReader (the app is named `EchoReader` internally, the repo is `Live_Audio_Notes`) is an Android study companion. You start a "reading session" for a book or article, then either read the text out loud or paste it in. As chunks of text come in, the app sends them to an LLM which returns:

- A detailed summary of the chunk
- Key points, connections to other ideas, and reflective questions
- Vocabulary/jargon with definitions
- Suggested topics to research further
- Auto-generated flashcards for spaced review
- A running "master notes" document that evolves across the whole session

On top of that, it layers a gamification system (XP, levels, streaks, achievements), a quiz mode built from your flashcards, a "Book Club" AI chat, a leaderboard, and PDF export of your notes.

## How it works

```
┌────────────────────┐        ┌──────────────────────────┐        ┌───────────────┐
│  Android App         │  HTTP  │  Node/Express Proxy       │  HTTP  │  Groq API      │
│  (Jetpack Compose)   │ ─────▶ │  (backend/server.js,      │ ─────▶ │  (LLM + Whisper│
│  Speech-to-Text via  │        │  deployed on Render)      │        │  transcription)│
│  Android SpeechRec.  │ ◀───── │  caches identical prompts │ ◀───── │                │
└──────────┬───────────┘        └──────────────────────────┘        └───────────────┘
           │  falls back to calling Groq directly if the proxy is unreachable
           ▼
   Room database (sessions, chunks, flashcards, stats) stored on-device
```

1. **Speech capture** — `VoiceReaderManager` wraps Android's built-in `SpeechRecognizer` for continuous, auto-restarting live transcription (with graceful error handling for network/audio issues).
2. **Local compression** — Before a chunk is sent over the network, `LocalPromptCompressor` strips URLs, filler phrases, and stop-words to save tokens on longer passages.
3. **Analysis** — `GroqClient` posts the chunk (plus the session's running master notes, reading purpose, depth level, and focus area) to the backend proxy's `/api/analyze` endpoint. The proxy calls an OpenAI-compatible model on Groq and hashes+caches identical requests for 2 hours to save tokens. If the proxy is down, the app calls Groq directly using the user's own API key as a fallback.
4. **Persistence** — Sessions, text chunks, and flashcards are stored locally in a Room database (`AppDatabase`, `SessionRepository`) so notes survive between app launches.
5. **Gamification** — Every completed session/chunk awards XP via `GamificationHelper`, which levels the user up (`100 + (level-1) * 50` XP per level) and unlocks achievements (e.g. Level 5 = "Dedicated Reader", Level 10 = "Scholar").

## Features

- 🎙️ **Live voice-to-notes** — continuous speech recognition that keeps listening across pauses
- ⌨️ **Paste-to-notes** — works just as well with typed/pasted text if you'd rather not talk
- 🧠 **Configurable analysis** — choose a reading purpose (Exam Prep, Research/Synthesis, Professional Growth, Leisure), a depth level (Beginner/Intermediate/Expert), and a focus area (Themes & Motifs, Logical Arguments, Characters & Narrative, Facts & Terms, General Analytics)
- 📝 **Evolving master notes** — each new chunk's analysis is folded into a running summary for the whole book/session
- 🗂️ **Session history** — revisit past reading sessions and their notes
- 🃏 **Auto-generated flashcards** and a **quiz mode** to test recall
- 🏆 **Gamification** — XP, levels, streaks, achievements, and a leaderboard screen
- 💬 **AI "Book Club"** — chat with an AI about the book you're reading
- 📄 **PDF export** of a session's full analysis
- 🔑 **Bring-your-own-key** — the app can use a proxy backend or your personal Groq API key directly

## Tech stack

**Android app** (`/app`)
- Kotlin + Jetpack Compose (Material 3), single-Activity/Crossfade-based screen routing (no Navigation-Compose dependency wired up)
- Room for local persistence (sessions, text chunks, study cards, user stats, achievements)
- OkHttp for networking, `android.speech.SpeechRecognizer` for on-device/system STT
- Firebase (App Check w/ reCAPTCHA, Firebase AI dependency present) and Moshi/Retrofit are included in the build but not used everywhere — see `app/build.gradle.kts` for what's actually wired up
- Gradle Kotlin DSL, AGP 9.1.1, Kotlin 2.2.10, `minSdk 24` / `targetSdk 36`

**Backend proxy** (`/backend`)
- Node.js + Express
- `openai` SDK pointed at Groq's OpenAI-compatible endpoint (`https://api.groq.com/openai/v1` by default, configurable via `LITELLM_BASE_URL`)
- `node-cache` for a 2-hour in-memory response cache (keyed by a hash of the request payload)
- `multer` for handling audio uploads to the Whisper transcription endpoint
- Requires the caller to supply their own API key via `Authorization: Bearer <key>` — the server itself holds no secret key

## Repository structure

```
Live_Audio_Notes/
├── app/                          # Android application
│   └── src/main/java/com/example/
│       ├── MainActivity.kt       # Screen routing (Crossfade over AppScreen enum)
│       ├── data/                 # Room entities, DAO, repository, gamification models
│       ├── network/               # GroqClient (LLM calls), LocalPromptCompressor
│       ├── speech/               # VoiceReaderManager (SpeechRecognizer wrapper)
│       ├── ui/screens/           # Onboarding, Goals setup, Session, History, Quiz,
│       │                         # Book Club, Leaderboard, Profile, API key screens
│       ├── ui/theme/             # Compose theme/colors/typography
│       ├── utils/PdfExporter.kt  # Exports a session's analysis to PDF
│       └── viewmodel/            # EchoReaderViewModel (app state), GamificationHelper
├── backend/                       # Express proxy for the Groq API
│   └── server.js
├── assets/                        # Static assets/screenshots
├── build.gradle.kts, settings.gradle.kts, gradle/  # Gradle build config
└── *.sh, *.py, *.kt (repo root)   # One-off maintenance/patch scripts used during
                                    # development (not part of the shipped app)
```

> **Note:** the collection of `fix_*.sh`, `patch_*.sh`, and loose `.kt`/`.py` files at the repo root are ad-hoc scripts that were used to patch source files during development. They aren't part of the app's build or runtime and can be ignored (or cleaned up) if you're just trying to run the project.

## Getting started

### Prerequisites
- [Android Studio](https://developer.android.com/studio) (recent version, AGP 9.1.1 / Kotlin 2.2.10 compatible)
- A [Groq API key](https://console.groq.com/) (the app uses Groq's OpenAI-compatible chat + Whisper transcription endpoints)
- Node.js 18+ if you want to run the backend proxy yourself

### 1. Run the Android app

1. Clone the repo and open it in Android Studio.
2. Let Android Studio sync Gradle and resolve any suggested fixes.
3. Create a `.env` file in the project root (see `.env.example`) and set:
   ```
   GROQ_API_KEY=your_actual_groq_api_key
   ```
   This is picked up at build time by the Secrets Gradle Plugin and exposed as `BuildConfig.GROQ_API_KEY`. Alternatively, you can skip this and enter an API key directly in the app's own onboarding/API-key screen at runtime — it's stored in `SharedPreferences` and used in place of the build-time key.
4. In `app/build.gradle.kts`, the `debug` build type currently points at `signingConfigs.getByName("debugConfig")`, which reads `debug.keystore` from the project root. If that file isn't present, either add one or point the config at Android Studio's default debug keystore.
5. Run the app on an emulator or physical device (grant the microphone permission when prompted to use live voice mode).

By default, the app talks to a hosted proxy (`https://live-audio-notes.onrender.com`) and falls back to calling Groq directly with your key if that proxy is unreachable. If you don't want to depend on that hosted instance, run your own backend (below) and update `PROXY_URL` in `app/src/main/java/com/example/network/GroqClient.kt`, or simply rely on the direct-Groq fallback path.

### 2. Run the backend proxy (optional, for self-hosting)

```bash
cd backend
npm install
npm start        # or: npm run dev (nodemon, auto-restart)
```

The server listens on `PORT` (default `3000`) and exposes:

| Endpoint | Method | Purpose |
|---|---|---|
| `/api/status` | GET | Health check / shows the upstream base URL in use |
| `/api/analyze` | POST | Structured note analysis for a text chunk (cached 2h) |
| `/api/voice/transcribe` | POST | Audio file → text via Whisper (multipart upload) |
| `/api/generate` | POST | Raw pass-through chat completion |

Every request except `/api/status` requires an `Authorization: Bearer <GROQ_API_KEY>` header — the server does not store or supply its own key. Optionally set `LITELLM_BASE_URL` to point at a different OpenAI-compatible provider/proxy, and `PORT` to change the listen port.

## Permissions

The app requests:
- `INTERNET` — to call the backend/Groq
- `RECORD_AUDIO` — for live voice transcription (requested at runtime; the app falls back to text-paste input if denied)

## Known limitations

- The app's model IDs default to Groq's `llama-3.1-8b-instant`; verify current Groq model availability if calls start failing.
- There's no automated CI/release pipeline evident beyond the standard `.github` workflow directory — check `.github/workflows` for what's actually configured.
- No LICENSE file is currently included in the repository.

## Contributing

Issues and PRs are welcome. If you're touching the analysis prompt or the note schema, keep `backend/server.js` and `app/.../network/GroqClient.kt` in sync — the app's direct-Groq fallback duplicates the same system prompt and expected JSON shape used by the proxy.
