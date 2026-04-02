# HSK学习 — HSK 4 Study App

A full-stack Chinese language learning application targeting HSK 3.0 Level 4. Features vocabulary drilling, reading comprehension, pronunciation assessment, and handwriting evaluation — all powered by Azure AI services.

## Architecture

```
hsk-learn/
├── backend/                        # .NET 10 Minimal API
│   └── HskLearn.Api/
│       ├── Program.cs              # App startup, DI, middleware
│       ├── Models/                 # Shared record types
│       ├── Services/               # Business logic
│       │   ├── VocabularyService       JSON/PDF vocab loader, search, pagination
│       │   ├── ReadingTaskService      Reading passage loader
│       │   ├── SpeechAssessmentService Azure Speech SDK (pronunciation + TTS)
│       │   ├── WritingEvaluationService Azure AI Foundry (vision model)
│       │   └── ProgressService         In-memory progress tracking
│       ├── Endpoints/              # Minimal API route groups
│       │   ├── /api/v1/vocabulary      Paged list, random, search, by-id
│       │   ├── /api/v1/reading         Passages, topics
│       │   ├── /api/v1/speech          Assess, assess-passage, synthesize, sentences
│       │   ├── /api/v1/writing         Evaluate handwriting
│       │   └── /api/v1/progress        Record attempts, summary, weak words
│       └── Data/                   # Seed JSON files
│           ├── hsk4_vocab.json         120 HSK 4 words
│           ├── reading_passages.json   10 reading passages with questions
│           └── practice_sentences.json 20 pronunciation practice sentences
│
├── android/                        # Native Android (Kotlin + Jetpack Compose)
│   └── app/
│       └── src/main/java/com/hsklearn/app/
│           ├── di/AppModule.kt         Hilt dependency injection
│           ├── data/
│           │   ├── api/HskApiService.kt    Retrofit interface (all endpoints)
│           │   ├── repository/             HskRepository, VocabularyRepository
│           │   └── model/                  @Serializable data classes
│           ├── ui/
│           │   ├── home/               Dashboard with progress tracking
│           │   ├── reading/            Passage display, quiz, read-aloud
│           │   ├── speaking/           Record + pronunciation assessment
│           │   ├── writing/            Handwriting canvas + AI evaluation
│           │   ├── components/         Shared: AudioPlayerHelper, OfflineBanner,
│           │   │                       PronunciationResultOverlay, NetworkMonitor
│           │   └── theme/              Dark theme (navy + gold accent)
│           └── navigation/             Bottom tab navigation
│
└── android/app/src/main/assets/    # HSK vocabulary PDFs + parsed JSON
    ├── vocabulary_band[1-5].pdf
    └── hsk_band[1-5].json
```

## Prerequisites

- [.NET 10 SDK](https://dotnet.microsoft.com/download)
- Android Studio (Hedgehog or later) with SDK 35
- Azure Speech Services subscription (for pronunciation assessment + TTS)
- Azure AI Foundry deployment (for handwriting evaluation)

## Backend Setup

### 1. Configure Azure credentials

Edit `backend/HskLearn.Api/appsettings.json`:

```json
{
  "AzureSpeech": {
    "SubscriptionKey": "your-speech-key",
    "Region": "your-region"
  },
  "AzureAIFoundry": {
    "Endpoint": "https://your-endpoint.openai.azure.com",
    "ApiKey": "your-ai-key",
    "DeploymentName": "your-gpt4o-deployment"
  }
}
```

> The vocabulary and reading modules work without Azure keys.
> Speech and writing evaluation require valid credentials.

### 2. Run the backend

```bash
cd backend/HskLearn.Api
dotnet run
```

The API starts at `http://localhost:5000`. Interactive docs are at `/scalar/v1`.

Health check: `GET /health` returns `{"status":"healthy","vocabCount":120}`.

### 3. API endpoints

| Group       | Endpoint                        | Method | Description                    |
|-------------|--------------------------------|--------|--------------------------------|
| Vocabulary  | `/api/v1/vocabulary`           | GET    | Paged word list                |
|             | `/api/v1/vocabulary/random`    | GET    | Random words                   |
|             | `/api/v1/vocabulary/search`    | GET    | Search by character/pinyin     |
|             | `/api/v1/vocabulary/{id}`      | GET    | Single word                    |
| Reading     | `/api/v1/reading/passage`      | GET    | Random passage (or ?topic=)    |
|             | `/api/v1/reading/topics`       | GET    | Available topics               |
| Speech      | `/api/v1/speech/assess`        | POST   | Pronunciation assessment       |
|             | `/api/v1/speech/assess-passage`| POST   | Full passage assessment        |
|             | `/api/v1/speech/synthesize`    | POST   | Text-to-speech (WAV)           |
|             | `/api/v1/speech/sentences`     | GET    | Random practice sentence       |
| Writing     | `/api/v1/writing/evaluate`     | POST   | Handwriting evaluation         |
| Progress    | `/api/v1/progress/record`      | POST   | Log a practice attempt         |
|             | `/api/v1/progress/summary`     | GET    | Overall progress stats         |
|             | `/api/v1/progress/weak-words`  | GET    | Lowest-scoring words           |

## Android Setup

### 1. Set the backend URL

In `android/app/build.gradle.kts`, update the `API_BASE_URL`:

```kotlin
// For Android emulator (localhost):
buildConfigField("String", "API_BASE_URL", "\"http://10.0.2.2:5000/\"")

// For a physical device on the same network:
buildConfigField("String", "API_BASE_URL", "\"http://192.168.x.x:5000/\"")
```

### 2. Build and run

Open the `android/` directory in Android Studio, sync Gradle, and run on a device or emulator.

**Target device:** Xiaomi Pad 8 Pro (arm64-v8a). The UI is tablet-optimised with landscape/portrait adaptive layouts.

### 3. Permissions

The app requests:
- `INTERNET` — API communication
- `RECORD_AUDIO` — pronunciation assessment recording
- `ACCESS_NETWORK_STATE` — offline detection

## Features

### Home Dashboard
- Today's date in Chinese, encouragement message
- HSK 4 vocabulary progress bar (X/1000 words)
- Reading / Speaking / Writing stat cards
- Quick practice shortcuts
- Weak words horizontal scroll

### Reading
- 10 HSK 4 reading passages across 7 topics
- Pinyin toggle, target word highlighting
- 3 comprehension questions per passage with explanations
- Read-aloud mode: TTS model pronunciation + recording + passage-level pronunciation assessment with colour-coded word results

### Speaking
- **Read Aloud tab:** practice sentences from the backend, scripted pronunciation assessment with per-word and per-phoneme scoring
- **Free Talk tab:** open-ended speaking prompts, unscripted assessment
- Pulsing record button, circular score ring, accuracy/fluency/completeness bars

### Writing
- Custom `HandwritingCanvasView` with 田字格 grid and pressure-sensitive ink
- Template mode (描红) renders target character as faint guide
- Undo/clear stroke controls
- AI evaluation via Azure AI Foundry vision model: stroke order, proportion, similarity, suggestions

## HSK Vocabulary Data

The `android/app/src/main/assets/` directory contains:
- `vocabulary_band[1-5].pdf` — Official HSK 3.0 vocabulary lists
- `hsk_band[1-5].json` — Parsed vocabulary (500-1071 words per band)

The backend uses `backend/HskLearn.Api/Data/hsk4_vocab.json` (120 seed words) for the API.

## Tech Stack

| Layer    | Technology                                         |
|----------|---------------------------------------------------|
| Backend  | .NET 10, Minimal APIs, Azure Speech SDK, Azure AI  |
| Android  | Kotlin 2.1, Jetpack Compose, Hilt, Retrofit, OkHttp|
| AI       | Azure Cognitive Services Speech, Azure AI Foundry   |
| Docs     | OpenAPI + Scalar                                    |
