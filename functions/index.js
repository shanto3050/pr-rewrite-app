const functions = require("firebase-functions");
const OpenAI = require("openai");

const STYLES = ["LINE", "TEAMS", "FORMAL"];
const MAX_TEXT_LENGTH = 2000;

const STYLE_PROMPTS = {
  LINE:
    "Rewrite the following text in a casual, friendly tone suitable for LINE or casual chat. Keep the same meaning; only change the tone. Output only the rewritten text, nothing else.",
  TEAMS:
    "Rewrite the following text in a professional, clear tone suitable for Microsoft Teams or work. Keep the same meaning; only change the tone. Output only the rewritten text, nothing else.",
  FORMAL:
    "Rewrite the following text in a formal, polite tone. Keep the same meaning; only change the tone. Output only the rewritten text, nothing else.",
};

exports.rewriteText = functions.https.onRequest(async (req, res) => {
  // CORS for Android app / future web
  res.set("Access-Control-Allow-Origin", "*");
  if (req.method === "OPTIONS") {
    res.set("Access-Control-Allow-Methods", "POST, OPTIONS");
    res.set("Access-Control-Allow-Headers", "Content-Type");
    return res.status(204).send("");
  }

  if (req.method !== "POST") {
    return res.status(405).json({ error: "Method Not Allowed" });
  }

  const { text, style } = req.body || {};
  if (!text || typeof text !== "string") {
    return res.status(400).json({ error: "Invalid or missing text" });
  }
  if (text.length > MAX_TEXT_LENGTH) {
    return res.status(400).json({
      error: `Text must be at most ${MAX_TEXT_LENGTH} characters`,
    });
  }
  if (!STYLES.includes(style)) {
    return res.status(400).json({
      error: "Invalid style. Use LINE, TEAMS, or FORMAL",
    });
  }

  const apiKey = functions.config().openai?.api_key;
  if (!apiKey) {
    console.error("OpenAI API key not configured");
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
    const rewritten =
      completion.choices[0]?.message?.content?.trim() ?? "";
    return res.status(200).json({ rewritten });
  } catch (e) {
    console.error("OpenAI error:", e);
    return res.status(500).json({ error: "Rewrite failed" });
  }
});
