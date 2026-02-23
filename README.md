# Rewrite AI (Android)

Native Android (Kotlin) app that rewrites selected or copied text in different tones (LINE / Teams / Formal) via OpenAI. The API key is never stored on the device—the app calls a **rewrite API** (hosted by you on Vercel or Firebase).

See **[docs/REWRITE_AI_ANDROID_PLAN.md](docs/REWRITE_AI_ANDROID_PLAN.md)** for architecture and design.

---

## Quick start (recommended: Vercel, no Firebase, no cost)

**You don’t need Firebase.** Use Vercel’s free tier to host the rewrite API:

**→ [Vercel setup guide (step-by-step)](docs/VERCEL_SETUP.md)**

In short: get an OpenAI API key → run `npm install` and `npx vercel` → add `OPENAI_API_KEY` in Vercel → set your Vercel URL in the Android app’s `strings.xml` → build and run the app. No Firebase, no billing.

---

## Other options

- **[Firebase setup](docs/FIREBASE_SETUP.md)** — If you prefer Firebase: Spark for the app only, then either **Vercel** or **local emulator** for the API (no Blaze), or **Blaze** to run the API on Firebase.

---

## How to use

- **Overlay:** Open the app from the launcher → tap “Open rewrite overlay”. The rewrite panel opens; paste or type text, pick a style (LINE / Teams / Formal), then Copy or Replace. Close with the X button or the back key.
- **From selected text:** In any app, select text → choose **Rewrite AI** from the share/copy menu. The panel opens with that text; use **Replace** to put the rewritten text back into the original app.

---

## Project layout

- **`app/`** — Android (Kotlin, Jetpack Compose): overlay service, rewrite UI, ProcessText, MainActivity.
- **`api/`** — Vercel serverless function: `rewriteText` (same behavior as the Firebase function).
- **`functions/`** — Firebase Cloud Functions (Node.js), optional if you use Firebase.
- **`docs/`** — Setup guides and architecture.

---

## Tech stack

- **Android:** Kotlin, Jetpack Compose, Coroutines, Retrofit (HTTP to the rewrite API).
- **Rewrite API:** Vercel (free) or Firebase Cloud Functions; OpenAI API (e.g. `gpt-4o-mini`).
