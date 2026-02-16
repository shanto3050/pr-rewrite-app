# Rewrite AI — Native Android (Kotlin) + Firebase — Detailed Plan

## 1. Understanding Summary

### What We're Building
A **Rewrite AI** Android app that:
- Lets users select or copy text from any app (LINE, KakaoTalk, etc.).
- Opens a **non-intrusive overlay** (floating UI) to rewrite that text in different tones via OpenAI.
- **Does not store the OpenAI API key on the device** — all AI calls go through Firebase Cloud Functions.

### Trigger Methods
| Method | How it works |
|--------|----------------|
| **A. Floating bubble** | App starts a Foreground Service → persistent draggable bubble (chat-head style). Tap → expand Rewrite UI. |
| **B. Context menu (PROCESS_TEXT)** | User selects text → system shows "Rewrite AI" in the share/copy menu → tap opens Rewrite UI with that text pre-loaded. |

### Rewrite UI (Overlay)
- **Floating overlay or bottom sheet** — does not open a full-screen Activity.
- **Input:** Editable field with original text.
- **Style buttons:** LINE (Casual), Teams (Professional), Formal.
- **Output cards:** One per style; each card is editable and has:
  - **Replace** — send result back to the calling app (PROCESS_TEXT result).
  - **Copy** — copy to clipboard and close UI.
  - **Regenerate** — call API again for that style only (temperature 0.7 for variation).

### Lifecycle
- Foreground Service with **persistent notification** (required to stay alive).
- **Dismiss:** Drag bubble to an "X" zone at bottom → `stopSelf()`, remove overlay, clear notification.

### Backend
- **Firebase Cloud Functions** (Node.js): HTTP endpoint.
- Payload: `{ "text": "draft message", "style": "LINE" }` (or "TEAMS", "FORMAL").
- API key from Firebase config/env; temperature 0.7; returns `{ "rewritten": "..." }`.

---

## 2. Architecture Overview

```
┌─────────────────────────────────────────────────────────────────┐
│                        ANDROID APP                               │
├─────────────────────────────────────────────────────────────────┤
│  Entry points:                                                   │
│  • MainActivity (start bubble service)                           │
│  • ProcessTextActivity (PROCESS_TEXT → overlay + optional result) │
├─────────────────────────────────────────────────────────────────┤
│  RewriteOverlayService (Foreground Service)                      │
│  • WindowManager + TYPE_APPLICATION_OVERLAY                      │
│  • Bubble View (Compose)  ←→  Expanded Rewrite UI (Compose)     │
│  • State: bubble position, expanded/collapsed, initial text       │
├─────────────────────────────────────────────────────────────────┤
│  RewriteViewModel / Use cases                                    │
│  • Call Firebase Cloud Function (rewrite API)                   │
│  • Manage styles, output cards, loading, errors                  │
├─────────────────────────────────────────────────────────────────┤
│  Firebase (Android SDK)                                          │
│  • Callable/HTTPS Callable for rewrite                           │
└─────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────┐
│              FIREBASE CLOUD FUNCTIONS (Node.js)                   │
│  • rewriteText(req: { text, style })                             │
│  • Read OPENAI_API_KEY from env                                  │
│  • Call OpenAI API (temperature 0.7)                             │
│  • Return { rewritten: string }                                  │
└─────────────────────────────────────────────────────────────────┘
```

---

## 3. Module-by-Module Build Plan

### Phase 1: Project Setup & Permissions

1. **Create Android project**
   - Kotlin, minSdk 24+, target 34.
   - Jetpack Compose, Material3.
   - Dependencies: `androidx.lifecycle:lifecycle-viewmodel-compose`, `org.jetbrains.kotlinx:kotlinx-coroutines-android`, Firebase BOM, optional `ktor`/`retrofit` for HTTP if not using Firebase Callable.

2. **AndroidManifest.xml**
   - **Overlay:** `SYSTEM_ALERT_WINDOW` (request at runtime on Android 6+).
   - **Foreground service:** `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_SPECIAL_USE` (or type overlay/connected if targeting 14+).
   - **Process text:** Activity with `android.intent.action.PROCESS_TEXT`, `mimeType="text/plain"`, `android:exported="true"`.
   - Optional: `INTERNET`, `POST_NOTIFICATIONS` (Android 13+).

3. **Firebase**
   - Create project in Firebase Console, add Android app, download `google-services.json`.
   - Enable Cloud Functions (Blaze plan for outbound HTTP to OpenAI).

---

### Phase 2: Firebase Cloud Function

4. **Initialize Functions**
   - `firebase init functions` (Node.js, TypeScript or JS).
   - Set env: `firebase functions:config:set openai.api_key="sk-..."` or use Secret Manager for production.

5. **Implement HTTP endpoint**
   - One function, e.g. `rewriteText`.
   - Validate `text` (string, max length e.g. 2000) and `style` (one of LINE, TEAMS, FORMAL).
   - Load API key from config or Secret Manager.
   - Call OpenAI (e.g. `gpt-4o-mini` or `gpt-3.5-turbo`) with a system prompt that maps style → tone (Casual / Professional / Formal).
   - Temperature 0.7.
   - Return `{ rewritten: string }`; on error return appropriate HTTP status and message.

6. **Prompts (example)**
   - LINE: “Rewrite in a casual, friendly tone suitable for LINE.”
   - TEAMS: “Rewrite in a professional, clear tone suitable for Microsoft Teams.”
   - FORMAL: “Rewrite in a formal, polite tone.”

---

### Phase 3: Foreground Service & Overlay

7. **RewriteOverlayService**
   - `startForeground()` with a notification (channel “Rewrite AI”, low importance or minimal).
   - Create `WindowManager` and add a view (ComposeView or Compose-based view) with type `TYPE_APPLICATION_OVERLAY`, flags for touchable and not focus-stealing as needed.
   - Layout params: `WRAP_CONTENT`, gravity or initial position (e.g. top-right).

8. **Bubble UI (Compose)**
   - Small circle/rounded view; use `Modifier.pointerInput` (or `detectDragGestures`) to update position and write to `WindowManager.LayoutParams`.
   - On tap: either expand in-place or show the Rewrite panel (see below).
   - Optional: “X” zone at bottom; when bubble is dropped there, call `stopSelf()` and remove the window.

9. **Rewrite UI (Compose)**
   - Shown when bubble is tapped or when opened via PROCESS_TEXT.
   - **Input:** `TextField`/`OutlinedTextField` with initial text (from intent or clipboard).
   - **Style buttons:** Three chips/buttons (LINE, Teams, Formal); on click call ViewModel → Cloud Function.
   - **Output cards:** For each style that has been requested, show a card with:
     - Editable text (output from API).
     - Buttons: Replace, Copy, Regenerate.
   - Loading/error states per style.
   - Close button to collapse back to bubble (or dismiss if from PROCESS_TEXT and no bubble).

10. **State management**
    - ViewModel (or a simple state holder) holds: `originalText`, `results: Map<Style, ResultState>` (Loading / Success(text) / Error).
    - Single flow or StateFlow for UI; collect in Compose.

---

### Phase 4: PROCESS_TEXT Integration

11. **ProcessTextActivity**
    - `android.intent.action.PROCESS_TEXT`, `EXTRA_PROCESS_TEXT` = selected text (CharSequence).
    - Start `RewriteOverlayService` with the selected text (e.g. Intent extra), and set a flag so the overlay shows expanded and with “Replace” enabled.
    - **Replace:** When user taps Replace, you must return the chosen text to the system. With PROCESS_TEXT you do this by calling `setResult(RESULT_OK, Intent().apply { putExtra(Intent.EXTRA_PROCESS_TEXT, rewrittenText) })` and `finish()` from the Activity that was started with PROCESS_TEXT. So the overlay must either (a) be hosted by this Activity, or (b) communicate back to the Activity to set result and finish. Easiest: Activity has a small transparent window or just starts the service and finishes; then a “Replace” action is only possible if we still have a reference to the ProcessTextActivity. **Important:** For Replace to work, the Activity that received PROCESS_TEXT must still be alive and call `setResult` + `finish()`. So: don’t finish the Activity immediately; keep it alive (e.g. transparent) and show the overlay from the service; when user taps Replace, have the service send the selected text back (e.g. via broadcast or callback) to the Activity so it can setResult and finish.

12. **Replace flow (refined)**
    - Option A: ProcessTextActivity is transparent and does not finish. It starts the overlay service with the selected text and a “result token”. Overlay shows; when user taps Replace, service tells the Activity (e.g. via Binder or broadcast) which text to return; Activity calls `setResult(RESULT_OK, Intent().putExtra(Intent.EXTRA_PROCESS_TEXT, text))` and `finish()`.
    - Option B: ProcessTextActivity adds an overlay view itself (same Compose UI) and when Replace is tapped, setResult and finish. No need for the bubble in this path.

---

### Phase 5: Polish & Edge Cases

13. **Copy**
    - Copy chosen card text to `ClipboardManager`; then collapse overlay or finish Activity.

14. **Regenerate**
    - Same style, same input; call Cloud Function again; replace that card’s content (temperature 0.7 gives new variation).

15. **Bubble dismiss**
    - Drag to “X” → remove overlay view, stopForeground, stopSelf().

16. **Back key / outside tap**
    - Collapse to bubble or close overlay consistently; if from PROCESS_TEXT, consider keeping Activity alive until user explicitly Replace/Copy or dismiss.

---

## 4. Suggested Project Structure (Android)

```
app/
├── src/main/
│   ├── AndroidManifest.xml
│   ├── java/.../rewriteai/
│   │   ├── MainActivity.kt
│   │   ├── ProcessTextActivity.kt
│   │   ├── service/
│   │   │   └── RewriteOverlayService.kt
│   │   ├── ui/
│   │   │   ├── bubble/
│   │   │   │   ├── BubbleView.kt
│   │   │   └── BubbleViewModel.kt (optional)
│   │   │   ├── rewrite/
│   │   │   │   ├── RewriteScreen.kt
│   │   │   │   ├── RewriteViewModel.kt
│   │   │   │   └── RewriteUiState.kt
│   │   ├── data/
│   │   │   ├── RewriteRepository.kt
│   │   │   └── FirebaseRewriteApi.kt (or Callable wrapper)
│   │   └── di/ (optional, e.g. Hilt)
│   └── res/
│       └── values/ (strings, themes)
functions/
├── src/
│   └── index.ts (or index.js)
│   └── rewrite.ts
├── package.json
└── .env or Firebase config for OPENAI_API_KEY
```

---

## 5. Firebase Cloud Function (Sketch)

```javascript
// functions/index.js
const functions = require("firebase-functions");
const OpenAI = require("openai");

const STYLES = ["LINE", "TEAMS", "FORMAL"];
const STYLE_PROMPTS = {
  LINE: "Rewrite the following text in a casual, friendly tone suitable for LINE or casual chat.",
  TEAMS: "Rewrite the following text in a professional, clear tone suitable for Microsoft Teams or work.",
  FORMAL: "Rewrite the following text in a formal, polite tone.",
};

exports.rewriteText = functions.https.onRequest(async (req, res) => {
  if (req.method !== "POST") {
    return res.status(405).send("Method Not Allowed");
  }
  const { text, style } = req.body || {};
  if (!text || typeof text !== "string" || !STYLES.includes(style)) {
    return res.status(400).json({ error: "Invalid text or style" });
  }
  const apiKey = functions.config().openai?.api_key;
  if (!apiKey) {
    return res.status(500).json({ error: "Server configuration error" });
  }
  const openai = new OpenAI({ apiKey });
  try {
    const completion = await openai.chat.completions.create({
      model: "gpt-4o-mini",
      messages: [
        { role: "system", content: STYLE_PROMPTS[style] },
        { role: "user", content: text },
      ],
      temperature: 0.7,
      max_tokens: 1024,
    });
    const rewritten = completion.choices[0]?.message?.content?.trim() ?? "";
    return res.status(200).json({ rewritten });
  } catch (e) {
    console.error(e);
    return res.status(500).json({ error: "Rewrite failed" });
  }
});
```

---

## 6. Key Android Implementation Notes

- **Overlay type:** `TYPE_APPLICATION_OVERLAY` (API 26+); permission `Settings.canDrawOverlays(context)`.
- **LayoutParams:** Use `WindowManager.LayoutParams` and update `x`, `y` on drag; use `FLAG_NOT_FOCUSABLE` if you don’t want to steal focus from the underlying app.
- **PROCESS_TEXT result:** Only the Activity that was started with `ACTION_PROCESS_TEXT` can return the result; keep that Activity alive until Replace or dismiss.
- **Notification:** Use `NotificationCompat` and a channel; `startForeground(NOTIFICATION_ID, notification)`.

---

## 7. New Ideas & Enhancements

1. **Favorites / history**  
   Store last N (original → style → rewritten) in Room or DataStore for quick re-use or “use again” from history.

2. **Custom styles**  
   Let users define custom style names and prompt snippets; store in Firestore or locally and pass a “custom” style with a prompt to the Cloud Function.

3. **Language detection / target language**  
   Add a “Translate & rewrite” mode: detect language, add target language to the prompt, return rewritten text in the desired language and tone.

4. **Rate limiting & auth**  
   In Cloud Functions, use Firebase Auth ID token in the request and enforce per-UID rate limits and optional quota to avoid abuse.

5. **Offline / cache**  
   Cache last result per (text, style) in memory or disk; show “Cached” and allow “Regenerate” to force a new call.

6. **Accessibility**  
   Ensure overlay and bubble have content descriptions and support TalkBack; consider larger touch targets for the bubble.

7. **Monet / theming**  
   Follow Material You (Material3) and system accent for the overlay so it feels native.

8. **Analytics**  
   Log (anonymized) usage of styles and Replace/Copy/Regenerate in Firebase Analytics to tune prompts and UX.

---

## 8. Build Order Summary

1. New Android (Kotlin + Compose) project + Firebase Android app.
2. Cloud Function: `rewriteText` with OpenAI, env config, and style prompts.
3. Overlay permission + Foreground Service + minimal notification.
4. Bubble view (draggable, tap to expand).
5. Rewrite UI (input, 3 style buttons, output cards, Copy/Regenerate).
6. Wire ViewModel to Cloud Function (HTTPS or Callable).
7. ProcessTextActivity: receive text, start service or show overlay with text; implement Replace via setResult.
8. Dismiss bubble to X → stopSelf.
9. Polish: loading, errors, Copy, Regenerate, and optional enhancements above.

This plan gives a clear understanding and a concrete path to build the Rewrite AI Android app with Kotlin, Compose, and Firebase.
