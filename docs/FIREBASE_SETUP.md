# Firebase setup — Rewrite AI

Step-by-step instructions to create the Firebase project, add the Android app, and connect the app to the rewrite API.

---

## No Blaze plan? You have two options

On the **free (Spark) plan**, Firebase **does not allow** Cloud Functions to call external APIs like OpenAI. So you can’t run the rewrite API on Firebase without upgrading to Blaze.

You can still use the app without paying:

| Option | Best for | What you do |
|--------|----------|-------------|
| **A. Vercel (free)** | A real URL for your phone/emulator, no billing | Deploy the same rewrite logic to Vercel; point the app at that URL. See **[Option A: Deploy the rewrite API on Vercel (free)](#option-a-deploy-the-rewrite-api-on-vercel-free)** below. |
| **B. Local emulator** | Development on your machine only | Run the Firebase Functions emulator on your PC; point the app at your computer’s URL. See **[Option B: Run the API locally (emulator)](#option-b-run-the-api-locally-emulator)** below. |

You still need a **Firebase project (Spark is fine)** only for the Android app: add the Android app and put `google-services.json` in `app/`. You do **not** need to deploy any Cloud Functions or enable Blaze if you use Option A or B.

---

## Option A: Deploy the rewrite API on Vercel (free)

The project includes a Vercel serverless function that does the same thing as the Firebase function. No billing, no Blaze.

1. **Sign up / log in** at [vercel.com](https://vercel.com) (free account).
2. **Install Vercel CLI** (optional; you can also use the website to deploy):
   ```bash
   npm install -g vercel
   ```
3. **Install dependency and deploy from the project root:**
   ```bash
   cd e:\Projects\pr_fe_master
   npm install
   vercel
   ```
   Follow the prompts (link to your account, project name). After deploy you’ll get a URL like `https://your-project-xxx.vercel.app`.
4. **Set your OpenAI API key** in Vercel:
   - Vercel dashboard → your project → **Settings** → **Environment Variables**.
   - Add: Name `OPENAI_API_KEY`, Value `sk-your-openai-key`, apply to Production (and Preview if you want).
5. **Redeploy** so the new env is used (e.g. run `vercel --prod` or push a small change and let Vercel redeploy).
6. **In the Android app**, set the rewrite base URL to your **API base** (the path is `/api/rewriteText`):
   - Open `app/src/main/res/values/strings.xml`.
   - Set `firebase_rewrite_base_url` to:
     ```xml
     <string name="firebase_rewrite_base_url" translatable="false">https://your-project-xxx.vercel.app/api</string>
     ```
   Replace with your real Vercel URL. The app will call `.../api/rewriteText` automatically.

You’re done. The rewrite API runs on Vercel’s free tier; no Firebase Blaze needed.

---

## Option B: Run the API locally (emulator)

Good for development: run the rewrite logic on your computer and call it from the Android emulator or a device on the same Wi‑Fi.

1. **Firebase project** — Create one on Spark (no Blaze). Add the Android app and put `google-services.json` in `app/` (see sections 1 and 3 below; skip section 2 and 4).
2. **Start the Functions emulator** (uses the same `functions/` code, but runs on your machine):
   ```bash
   cd e:\Projects\pr_fe_master
   firebase init emulators
   ```
   Choose **Functions** only, accept defaults. Then:
   ```bash
   firebase functions:config:get
   ```
   If you haven’t set the OpenAI key in Firebase config, set it locally. Create `functions/.runtimeconfig.json` (do **not** commit this file):
   ```json
   {
     "openai": {
       "api_key": "sk-your-openai-api-key"
     }
   }
   ```
   Start the emulator:
   ```bash
   firebase emulators:start --only functions
   ```
   You’ll see something like: `rewriteText: http://127.0.0.1:5001/YOUR_PROJECT_ID/us-central1/rewriteText`.
3. **Point the Android app at your PC:**
   - From the **Android emulator**, use `10.0.2.2` instead of `localhost` (it’s the host machine).
   - Base URL (replace `YOUR_PROJECT_ID` with your Firebase project ID):
     ```xml
     http://10.0.2.2:5001/YOUR_PROJECT_ID/us-central1
     ```
   - Put that in `app/src/main/res/values/strings.xml` as `firebase_rewrite_base_url`. The app will call `.../rewriteText`.
   - From a **physical device** on the same Wi‑Fi, use your PC’s local IP (e.g. `http://192.168.1.10:5001/YOUR_PROJECT_ID/us-central1`). You may need to allow the port through Windows Firewall.

When you’re done developing, you can switch to Vercel (Option A) or Firebase Blaze and change the base URL in `strings.xml`.

---

## 1. Create a Firebase project

1. Go to **[Firebase Console](https://console.firebase.google.com/)** and sign in with your Google account.
2. Click **“Add project”** (or **“Create a project”**).
3. Enter a **project name** (e.g. `Rewrite AI`). You can disable Google Analytics if you don’t need it. Click **Continue** and then **Create project**.
4. When the project is ready, click **Continue**. You’ll land on the project overview.

**Note:** Remember the **Project ID** (e.g. `rewrite-ai-xxxxx`). You’ll need it for the Android app and for deploying functions.

---

## 2. (Optional) Upgrade to Blaze — only if you want to use Firebase for the rewrite API

If you use **Vercel** or the **local emulator** (Options A or B above), skip this step.

Cloud Functions that call external APIs (like OpenAI) require the **Blaze** plan. If you want to host the rewrite API on Firebase instead of Vercel:

1. In the left sidebar, click the **gear icon** next to “Project Overview” → **Usage and billing**.
2. Click **Modify plan** (or **Upgrade**).
3. Select **Blaze** and follow the prompts (add a billing account if needed).
4. You can set **budget alerts** and a **spend limit** so you’re not charged unexpectedly.

---

## 3. Add the Android app and get `google-services.json`

1. In the project overview, click the **Android** icon (or “Add app” → Android).
2. **Android package name:** enter exactly:
   ```text
   com.rewriteai
   ```
3. **App nickname (optional):** e.g. `Rewrite AI`.
4. **Debug signing certificate (optional):** you can skip for now. Click **Register app**.
5. **Download `google-services.json`:**
   - Click **Download google-services.json** and save the file.
   - Copy it into your project:
     ```text
     e:\Projects\pr_fe_master\app\google-services.json
     ```
   - If you already have `app/google-services.json.example`, replace or overwrite with this file.
6. Click **Next** until you’re done. You can ignore the “Add Firebase SDK” steps in the wizard; the project already has the Gradle config.

---

## 4. (Optional) Enable and configure Cloud Functions — only if you use Blaze

If you use **Vercel** or the **local emulator**, skip to section 5 and set `firebase_rewrite_base_url` to your Vercel or emulator base URL instead.

Enable and configure Cloud Functions (Blaze only):

### 4.1 Install Firebase CLI (if needed)

- **Option A — npm:**
  ```bash
  npm install -g firebase-tools
  ```
- **Option B — standalone:** see [Firebase CLI](https://firebase.google.com/docs/cli).

Log in:

```bash
firebase login
```

### 4.2 Link the project to your app

1. Open a terminal in the project root:
   ```bash
   cd e:\Projects\pr_fe_master
   ```
2. Log in and link the project:
   ```bash
   firebase login
   firebase use --add
   ```
3. When prompted, select your Firebase project from the list (or enter its Project ID). Give the alias a name (e.g. `default`). This updates `.firebaserc` with your project ID.

**Or** edit `.firebaserc` by hand:

```json
{
  "projects": {
    "default": "YOUR_ACTUAL_PROJECT_ID"
  }
}
```

Replace `YOUR_ACTUAL_PROJECT_ID` with the Project ID from the Firebase Console (e.g. `rewrite-ai-xxxxx`).

### 4.3 Install function dependencies and set the OpenAI key

```bash
cd functions
npm install
```

Set your OpenAI API key in Firebase config (do **not** commit this key to git):

```bash
firebase functions:config:set openai.api_key="sk-your-openai-api-key-here"
```

Use your real key from [OpenAI API keys](https://platform.openai.com/api-keys). The Cloud Function will read it with `functions.config().openai.api_key`.

### 4.4 Deploy the rewrite function

From the project root:

```bash
cd e:\Projects\pr_fe_master
firebase deploy --only functions
```

Wait until you see something like:

```text
✔  functions[rewriteText(us-central1)]: Successful create operation.
Function URL (rewriteText(us-central1)): https://us-central1-YOUR_PROJECT_ID.cloudfunctions.net/rewriteText
```

Copy that **Function URL** (or the base part without `/rewriteText`). You’ll use it in the Android app.

---

## 5. Point the Android app at the rewrite API

1. Open:
   ```text
   app/src/main/res/values/strings.xml
   ```
2. Find the string `firebase_rewrite_base_url`.
3. Set it to the **base URL** of your rewrite API (no trailing slash):
   - **Firebase (Blaze):**  
     `https://us-central1-YOUR_PROJECT_ID.cloudfunctions.net`
   - **Vercel:**  
     `https://your-project-xxx.vercel.app/api`  
     (The app will call `.../api/rewriteText`.)
   - **Local emulator (emulator):**  
     `http://10.0.2.2:5001/YOUR_PROJECT_ID/us-central1`  
     (Device on same Wi‑Fi: use your PC’s IP instead of `10.0.2.2`.)

Example (Firebase):

```xml
<string name="firebase_rewrite_base_url" translatable="false">https://us-central1-rewrite-ai-xxxxx.cloudfunctions.net</string>
```

The app always calls `base_url + "rewriteText"` for the rewrite API.

---

## 6. Quick checklist

**If you’re not using Blaze (recommended: use Vercel or emulator):**

| Step | What to do |
|------|------------|
| 1 | Create Firebase project (Spark is fine), note **Project ID** |
| 2 | Add Android app with package `com.rewriteai`, download **google-services.json** → put in `app/` |
| 3 | Deploy **Vercel** (Option A) or run **emulator** (Option B); get the base URL |
| 4 | In **strings.xml**, set **firebase_rewrite_base_url** to that base URL |

**If you are using Blaze (Firebase for the API):**

| Step | What to do |
|------|------------|
| 1 | Create Firebase project, upgrade to **Blaze** |
| 2 | Add Android app, download **google-services.json** → put in `app/` |
| 3 | `firebase use --add` or edit **.firebaserc** with project ID |
| 4 | In `functions/`: **npm install**, **firebase functions:config:set openai.api_key="sk-..."** |
| 5 | **firebase deploy --only functions**, copy function base URL |
| 6 | In **strings.xml**, set **firebase_rewrite_base_url** to that base URL |

After this, the Android app can call your rewrite API (Vercel, emulator, or Firebase) to rewrite text with OpenAI.

---

## 7. Optional: Check the function in the browser

You can test the HTTP function with a tool like Postman or curl:

```bash
curl -X POST "https://us-central1-YOUR_PROJECT_ID.cloudfunctions.net/rewriteText" \
  -H "Content-Type: application/json" \
  -d "{\"text\": \"Please send the report by Friday.\", \"style\": \"LINE\"}"
```

Expected response (status 200):

```json
{"rewritten":"Hey, could you send the report by Friday? Thanks!"}
```

(Exact text may vary; the function uses temperature 0.7.)

---

## 8. Troubleshooting

- **“Permission denied” or “Billing not enabled”**  
  Enable Blaze and ensure the project has a linked billing account.

- **“OpenAI API key not configured”**  
  Run `firebase functions:config:set openai.api_key="sk-..."` again, then redeploy: `firebase deploy --only functions`.

- **Android app gets 404 or wrong URL**  
  Confirm `firebase_rewrite_base_url` has no trailing slash and no `/rewriteText` path (the app adds `rewriteText` itself).

- **CORS errors when testing from a web page**  
  The function already sets `Access-Control-Allow-Origin: *` for POST/OPTIONS. If you still see CORS issues, they’re likely from the client or browser; the Android app does not use CORS.

If you want, the next step can be a short “First run” section (overlay permission, PROCESS_TEXT, and testing Replace/Copy) in the same doc or in the main README.
