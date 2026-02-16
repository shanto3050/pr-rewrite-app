/**
 * Vercel serverless function — same behavior as Firebase rewriteText.
 * Deploy with Vercel (free tier); set OPENAI_API_KEY in project settings.
 * No Blaze / billing required.
 */

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

module.exports = async (req, res) => {
  res.setHeader("Access-Control-Allow-Origin", "*");
  if (req.method === "OPTIONS") {
    res.setHeader("Access-Control-Allow-Methods", "POST, OPTIONS");
    res.setHeader("Access-Control-Allow-Headers", "Content-Type");
    return res.status(204).end();
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

  const apiKey = process.env.OPENAI_API_KEY;
  if (!apiKey) {
    console.error("OPENAI_API_KEY not set");
    return res.status(500).json({ error: "Server configuration error" });
  }

  const OpenAI = require("openai");
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
    console.error("OpenAI error:", e);
    return res.status(500).json({ error: "Rewrite failed" });
  }
};
