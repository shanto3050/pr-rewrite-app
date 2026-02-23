/**
 * Vercel serverless function — same behavior as Firebase rewriteText.
 * Deploy with Vercel (free tier); set OPENAI_API_KEY in project settings.
 * No Blaze / billing required.
 */

const STYLES = ["SUPER_CASUAL", "CASUAL_POLITE", "POLITE", "VERY_FORMAL"];
const MAX_TEXT_LENGTH = 2000;

const BASE_PROMPT =
  "You are a rewriting assistant. CRITICAL: Output MUST be in the SAME LANGUAGE as the user's input. " +
  "If the input is in English, respond ONLY in English. If the input is in Japanese, respond ONLY in Japanese. " +
  "Do NOT translate and do NOT switch language. Keep the same meaning. " +
  "Respond with ONLY the rewritten text, no quotes or explanation.";

const STYLE_PROMPTS = {
  SUPER_CASUAL:
    BASE_PROMPT +
    " Rewrite in a very casual, friendly tone (close friends / casual chat). " +
    "If the input is Japanese, use natural spoken casual Japanese (ため口).",
  CASUAL_POLITE:
    BASE_PROMPT +
    " Rewrite in a somewhat casual but still polite tone (colleagues / acquaintances). " +
    "If the input is Japanese, use soft 丁寧語.",
  POLITE:
    BASE_PROMPT +
    " Rewrite in a polite, professional tone (business emails/chats). " +
    "If the input is Japanese, use ビジネス敬語.",
  VERY_FORMAL:
    BASE_PROMPT +
    " Rewrite in a very formal, highly polite tone (important business / superiors / customers). " +
    "If the input is Japanese, use very polite 敬語.",
};

const REGENERATE_INSTRUCTION =
  "The user asked for another version. You MUST give a different phrasing: different wording, sentence structure, or expressions, while keeping the same tone and meaning. Do not repeat the previous answer. " +
  "IMPORTANT: Keep the exact same language as the user's input; do not translate or switch to another language.";

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

  const { text, style, regenerate } = req.body || {};
  if (!text || typeof text !== "string") {
    return res.status(400).json({ error: "Invalid or missing text" });
  }
  const isRegenerate = regenerate === true;
  if (text.length > MAX_TEXT_LENGTH) {
    return res.status(400).json({
      error: `Text must be at most ${MAX_TEXT_LENGTH} characters`,
    });
  }
  if (!STYLES.includes(style)) {
    return res.status(400).json({
      error: `Invalid style. Use one of: ${STYLES.join(", ")}`,
    });
  }

  const apiKey = process.env.OPENAI_API_KEY;
  if (!apiKey) {
    console.error("OPENAI_API_KEY not set");
    return res.status(500).json({ error: "Server configuration error" });
  }

  const OpenAI = require("openai");
  const openai = new OpenAI({ apiKey });
  const systemContent = isRegenerate
    ? STYLE_PROMPTS[style] + "\n\n" + REGENERATE_INSTRUCTION
    : STYLE_PROMPTS[style];
  try {
    const completion = await openai.chat.completions.create({
      model: "gpt-4o-mini",
      messages: [
        { role: "system", content: systemContent },
        { role: "user", content: text },
      ],
      temperature: isRegenerate ? 0.9 : 0.7,
      max_tokens: 1024,
    });
    const rewritten = completion.choices[0]?.message?.content?.trim() ?? "";
    return res.status(200).json({ rewritten });
  } catch (e) {
    console.error("OpenAI error:", e);
    return res.status(500).json({ error: "Rewrite failed" });
  }
};
