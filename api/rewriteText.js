/**
 * Vercel serverless function — same behavior as Firebase rewriteText.
 * Deploy with Vercel (free tier); set OPENAI_API_KEY in project settings.
 * No Blaze / billing required.
 */

const STYLES = ["SUPER_CASUAL", "CASUAL_POLITE", "POLITE", "VERY_FORMAL"];
const MAX_TEXT_LENGTH = 2000;

const STYLE_PROMPTS = {
  SUPER_CASUAL:
    "You are a rewriting assistant. Rewrite the text in a very casual, friendly tone suitable for close friends or casual chat. " +
    "Keep the SAME LANGUAGE as the input and keep the same meaning. " +
    "If the input is Japanese, use natural spoken casual Japanese (ため口). " +
    "Do NOT translate to a different language. Respond with the rewritten text only.",
  CASUAL_POLITE:
    "You are a rewriting assistant. Rewrite the text in a somewhat casual but still polite tone. " +
    "Keep the SAME LANGUAGE as the input and keep the same meaning. " +
    "If the input is Japanese, use soft and approachable 丁寧語 (polite Japanese) that you would use with colleagues or acquaintances. " +
    "Do NOT translate to a different language. Respond with the rewritten text only.",
  POLITE:
    "You are a rewriting assistant. Rewrite the text in a polite, professional tone suitable for business emails or chats. " +
    "Keep the SAME LANGUAGE as the input and keep the same meaning. " +
    "If the input is Japanese, use appropriate ビジネス敬語 (business polite Japanese). " +
    "Do NOT translate to a different language. Respond with the rewritten text only.",
  VERY_FORMAL:
    "You are a rewriting assistant. Rewrite the text in a very formal, highly polite tone, suitable for important business, official documents, or communication with superiors or customers. " +
    "Keep the SAME LANGUAGE as the input and keep the same meaning. " +
    "If the input is Japanese, use very polite 敬語 expressions. " +
    "Do NOT translate to a different language. Respond with the rewritten text only.",
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
