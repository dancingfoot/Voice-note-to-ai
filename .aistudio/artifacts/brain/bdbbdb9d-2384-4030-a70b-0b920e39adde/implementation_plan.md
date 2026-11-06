# Monovox Voice Tuning & Gemini-Style Neural Audio

Upgrade Monovox's on-device synthesis engine to detect, prioritize, and configure Google's high-definition natural neural voices matching the Gemini app's voice profile, complete with an interactive Voice & Audio Studio configuration menu.

## User Review & Critical Decisions

> [!IMPORTANT]
> The following parameters were confirmed during clarification and will form the default audio profile for all spoken responses:

- **Confirmed Voice Persona**: Warm natural tone modeled after Gemini's signature **Aoede / Kore** voices (prioritizing Google Neural female network voices such as `en-us-x-sfg-network`).
- **Confirmed Cadence & Tempo**: Balanced, natural conversational pacing set to **1.05x rate** and **1.0x pitch**.
- **User Control in App**: Interactive Voice Tuning drawer/section inside Settings (`⚙️`) with real-time "Test Voice" auditioning, profile presets (Aoede/Warm, Puck/Conversational, Charon/Deep), and fine pitch/rate controls.

---

## 1. Overview & Core Concept

* **What It Does**: Enhances the audible reply pipeline by replacing generic robotic system speech with Google's high-definition Neural voice engine, calibrated to emulate the warm, natural cadence of Google's Gemini application.
* **Target Audience**: Users utilizing Monovox as a real-time hands-free audio assistant who desire fluid, human-like voice responses rather than mechanical synthetic speech.
* **Key Value**: Delivers the premium conversational feel of the official Gemini Live app while retaining complete multi-backend routing (DeepSeek, Gemini 3.8-Flash, and custom agent harnesses) and offline Room DB logging.

---

## 2. User Experience & Visual Design

* **Voice Tuning Section in Settings (`⚙️`)**:
  * Located immediately below the active backend endpoint selector in the Settings dialog.
  * **Preset Selector Pills**:
    * `[AOEDE / KORE (WARM)]` (Default, active)
    * `[PUCK (CONVERSATIONAL)]`
    * `[CHARON (DEEP / FOCUSED)]`
    * `[SYSTEM DEFAULT]`
  * **Interactive Audition Button**: A high-contrast `[▶ TEST VOICE]` button that speaks a crisp sample phrase (*"Hello! I am ready to assist with your queries and notes."*) so users can calibrate before returning to conversation.
  * **Fine-Tuning Controls**: Smooth minimalist sliders for Speed (`0.8x` to `1.4x`) and Pitch (`0.8x` to `1.2x`).
* **Seamless Audio Transitions**:
  * Persistent storage of chosen voice preferences across app restarts.
  * Instant barge-in: starting Push-to-Talk or tapping "Interrupt" immediately silences playback regardless of selected voice.

---

## 3. Key Product Decisions & Trade-Offs

* **Decision 1: Engine Prioritization (`com.google.android.tts`)**:
  * *Chosen Approach*: Specifically request the `com.google.android.tts` engine and enumerate `engine.voices` to filter for high-quality network and neural US English voices.
  * *Why*: Google's Speech Services engine ships on virtually all Android devices and provides the same neural voice models used by Google Assistant and Gemini.
  * *Alternative Considered*: Shipping an embedded multi-megabyte ONNX/Piper TTS model. Rejected because it would bloat the APK size from ~15MB to 80MB+ and increase battery drain.
* **Decision 2: Automatic Fallback**:
  * *Chosen Approach*: If a specific neural voice is unavailable offline or on a de-Googled ROM, the system seamlessly degrades to the highest quality installed system voice without errors.

---

## 4. Technical Architecture & Data Strategy

```
┌────────────────────────────────────────────────────────┐
│                   MainVoiceScreen                      │
│   (Conversation Canvas / Waveform / PTT / Stream)      │
└───────────────────────────┬────────────────────────────┘
                            │
                            ▼
┌────────────────────────────────────────────────────────┐
│               EndpointConfigDialog                     │
│   ┌────────────────────────────────────────────────┐   │
│   │  Backend Selector: DeepSeek / Gemini 3.8-Flash │   │
│   ├────────────────────────────────────────────────┤   │
│   │  NEW: Voice Tuning & Neural Preset Studio      │   │
│   │  [Aoede Warm]  [Puck Conv]  [Charon Deep]      │   │
│   │  Sliders: Rate (1.05x) | Pitch (1.0x)          │   │
│   │  [▶ Test Voice Audio Button]                   │   │
│   └────────────────────────────────────────────────┘   │
└───────────────────────────┬────────────────────────────┘
                            │ updates preferences
                            ▼
┌────────────────────────────────────────────────────────┐
│                     TtsManager                         │
│  - Engine: com.google.android.tts                      │
│  - Voice Filter: en-us-x-sfg-network / neural voices   │
│  - Pitch: 1.0f | Rate: 1.05f                           │
│  - Instant Barge-in / queue flush                      │
└────────────────────────────────────────────────────────┘
```

* **Data Model Updates**:
  * `VoicePreference` state stored in ViewModel and local preferences: `voiceName`, `pitch`, `rate`.
* **CI/CD & Delivery**:
  * Update `app/build.gradle.kts` to `versionCode = 3`, `versionName = "1.0.2"`.
  * Commit and push git tag `v1.0.2` to `dancingfoot/Voice-note-to-ai` so GitHub Actions automatically compiles and releases the updated APK.
