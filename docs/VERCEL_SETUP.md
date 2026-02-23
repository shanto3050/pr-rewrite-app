# Rewrite AI — Vercel-only setup (no Firebase, no cost)

If you use **Vercel** for the rewrite API, you **do not need Firebase at all**. The Android app talks to your Vercel URL over HTTP; there is no Firebase project, no `google-services.json`, and no billing.

Vercel’s free tier is enough for normal use. You only need:

- A **Vercel account** (free)
- An **OpenAI API key** (you pay OpenAI for usage; the rewrite calls are cheap with `gpt-4o-mini`)

---

## Step 1: Get an OpenAI API key

1. Go to [platform.openai.com](https://platform.openai.com) and sign in or create an account.
2. Open **API keys** and create a new key.
3. Copy the key (it starts with `sk-`). You’ll add it to Vercel in Step 5; the Android app never sees it.

---

## Step 2: Push the project to your GitHub repository

Vercel can deploy from the **Vercel CLI** (no GitHub required) or by **connecting a GitHub repo**. If you created an empty repo and Vercel is asking for it, push your project there first, then connect the repo in Vercel.

### 2a. Push your project to GitHub

1. **Open a terminal** in the project folder:
   ```bash
   cd e:\Projects\pr_fe_master
   ```

2. **Initialize Git** (if this folder is not yet a git repo):
   ```bash
   git init
   ```

3. **Add your GitHub repo as the remote** (replace with your actual repo URL):
   ```bash
   git remote add origin https://github.com/YOUR_USERNAME/YOUR_REPO_NAME.git
   ```
   Example: `https://github.com/jane/rewrite-ai.git`  
   Or with SSH: `git@github.com:YOUR_USERNAME/YOUR_REPO_NAME.git`

4. **Stage and commit** everything:
   ```bash
   git add .
   git commit -m "Initial commit: Rewrite AI Android + Vercel API"
   ```

5. **Push to GitHub** (use your main branch name; GitHub often uses `main`):
   ```bash
   git branch -M main
   git push -u origin main
   ```
   If your repo already has a branch (e.g. from a README), you might need:
   ```bash
   git pull origin main --allow-unrelated-histories
   git push -u origin main
   ```
   Or if the empty repo has no commits yet, the first push above is enough.

Your project is now on GitHub. Next, connect it in Vercel (Step 2b) or deploy with the CLI (Step 4).

### 2b. Connect the repo in Vercel (website)

1. Go to [vercel.com/new](https://vercel.com/new) (or **Add New… → Project**).
2. Under **Import Git Repository**, choose **GitHub** and authorize if asked.
3. Select your **repository** (e.g. `rewrite-ai`) from the list.
4. **Configure Project:**
   - **Framework Preset:** leave as **Other** (or **Vercel**).
   - **Root Directory:** leave as `.` (project root).
   - **Build Command:** leave empty (the `api/` folder is used as serverless functions; no build needed).
   - **Output Directory:** leave empty.
5. Click **Deploy**. Vercel will clone the repo and deploy. The first deploy may not have `OPENAI_API_KEY` yet—add it in **Settings → Environment Variables** (Step 5), then **Redeploy**.

Your live URL will be like `https://your-project.vercel.app`. Use that in Step 6 for the Android app (with `/api` at the end).

---

## Step 3: Install Node.js (if you don’t have it)

You need Node.js only if you deploy via the CLI (Step 4). If you used **Import** from GitHub (Step 2b), you can skip to Step 5.

- Download from [nodejs.org](https://nodejs.org) (LTS).
- Or with winget: `winget install OpenJS.NodeJS.LTS`

Check:

```bash
node -v
npm -v
```

---

## Step 4: Deploy the rewrite API to Vercel (if you didn’t use “Import” in Step 2b)

1. Open a terminal in the project folder:
   ```bash
   cd e:\Projects\pr_fe_master
   ```

2. Install the dependency:
   ```bash
   npm install
   ```

3. Log in to Vercel (opens browser):
   ```bash
   npx vercel login
   ```
   Use **Email** or **GitHub** and complete the login.

4. Deploy:
   ```bash
   npx vercel
   ```
   - **Set up and deploy?** → **Y**
   - **Which scope?** → your account
   - **Link to existing project?** → **N**
   - **Project name?** → e.g. `rewrite-ai` (or press Enter)
   - **Directory?** → `.` (press Enter)

   When it finishes you’ll see something like:

   ```text
   Production: https://rewrite-ai-xxxx.vercel.app [copied to clipboard]
   ```

   Copy that URL (without any path). You’ll use it in the next steps.

---

## Step 5: Add your OpenAI API key in Vercel (step-by-step)

Do this in the Vercel website. Your API key is only stored on Vercel’s servers; the Android app never sees it.

### 5.1 Open the Vercel dashboard

1. In your browser, go to **[vercel.com](https://vercel.com)** and log in.
2. You should see your **dashboard** with a list of projects.
3. Click the **project** that hosts the rewrite API (e.g. **pr-rewrite-appver10** or the name you gave it).

### 5.2 Open Environment Variables

1. At the top of the project page you’ll see tabs: **Deployments**, **Analytics**, **Logs**, **Settings**, etc.
2. Click **Settings**.
3. In the left sidebar under your project name, look for **Environment Variables**.
4. Click **Environment Variables**.

### 5.3 Add the OpenAI API key

1. You’ll see a form or table for adding variables. Find the **“Add New”** or **“Key”** / **“Value”** fields.
2. **Key (name):** type exactly:
   ```text
   OPENAI_API_KEY
   ```
   Use all caps and underscores; the API code looks for this exact name.
3. **Value:** paste your OpenAI API key (starts with `sk-...`).  
   Get it from [platform.openai.com](https://platform.openai.com) → **API keys** → **Create new secret key**.
4. **Environment:** select where this key is used:
   - Check **Production** (so your live URL uses it).
   - Optionally check **Preview** if you want preview deployments to work too.
5. Click **Save** (or **Add** / **Confirm**). The new variable should appear in the list.

### 5.4 Redeploy so the key is used

New environment variables are only applied on the **next** deploy. Existing deployments don’t see them.

1. Click the **Deployments** tab at the top.
2. Find the **latest deployment** (top of the list).
3. Click the **three dots (⋯)** or **“More”** on the right of that row.
4. Click **Redeploy** (or **Redeploy with existing build**).
5. Confirm if asked. Wait until the deployment status is **Ready**.

After this, your Vercel URL will use `OPENAI_API_KEY` for the rewrite API. You can test it from the app or with curl (see “Test the API” at the end of this doc).

---

## Step 6: Set the API URL in the Android app

The app calls your Vercel project at `/api/rewriteText`. So the “base URL” must include `/api`.

1. Open in your editor:
   ```text
   e:\Projects\pr_fe_master\app\src\main\res\values\strings.xml
   ```

2. Find the line with `firebase_rewrite_base_url` and set it to your Vercel URL **plus** `/api` (no trailing slash):

   **If your Vercel URL is**  
   `https://rewrite-ai-xxxx.vercel.app`

   **then set:**
   ```xml
   <string name="firebase_rewrite_base_url" translatable="false">https://rewrite-ai-xxxx.vercel.app/api</string>
   ```
   Replace `rewrite-ai-xxxx.vercel.app` with your real Vercel URL from the deploy step.

3. Save the file.

The app will call: `base_url + "rewriteText"` → `https://your-project.vercel.app/api/rewriteText`.

---

## Step 7: Build and run the Android app

1. Open the project in **Android Studio** (`e:\Projects\pr_fe_master`).
2. Wait for Gradle sync to finish (no `google-services.json` needed).
3. Connect a device or start an emulator (API 26+).
4. Run the app (green Run button or **Run → Run 'app'**).
5. Grant **“Display over other apps”** when you tap **Start bubble**.

You’re done. The app uses only your Vercel API; there is no Firebase involved.

---

## Quick checklist (Vercel only)

| Step | What to do |
|------|------------|
| 1 | Get **OpenAI API key** from platform.openai.com |
| 2 | **Push project to GitHub:** `git init` → `git remote add origin <repo-url>` → `git add .` → `git commit -m "..."` → `git push -u origin main` |
| 3 | **Vercel:** Either **Import** the repo at vercel.com/new, or run **npm install** + **npx vercel** in the project folder |
| 4 | In Vercel: **Settings → Environment Variables** → add **OPENAI_API_KEY** → **Redeploy** |
| 5 | In **strings.xml** set **firebase_rewrite_base_url** to `https://YOUR_VERCEL_URL/api` |
| 6 | Open project in **Android Studio**, run on device/emulator, grant overlay permission |

---

## Test the API (optional)

From a terminal (replace with your URL):

```bash
curl -X POST "https://your-project.vercel.app/api/rewriteText" -H "Content-Type: application/json" -d "{\"text\": \"Please send the report by Friday.\", \"style\": \"LINE\"}"
```

You should get JSON with a `rewritten` field.

---

## Vercel free tier (summary)

- **Serverless invocations:** Generous free tier; rewriting a few messages uses very little.
- **Bandwidth:** Enough for normal app use.
- **No credit card** required for the free tier.

You only pay **OpenAI** for API usage (e.g. a few cents per many rewrites with `gpt-4o-mini`). The app and Vercel setup described here do not require Firebase or any other paid service.
