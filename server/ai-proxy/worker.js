// Lanka Weather Guard AI helper proxy (Cloudflare Worker).
// Keeps the Gemini API key on the server so it is never shipped inside the app.
//
// POST { "question": "...", "context": "..." }  ->  { "answer": "..." }

const MAX_QUESTION = 500;
const MAX_CONTEXT = 6000;

export default {
  async fetch(request, env) {
    if (request.method !== "POST") {
      return json({ error: "method not allowed" }, 405);
    }

    let body;
    try {
      body = await request.json();
    } catch {
      return json({ error: "bad request" }, 400);
    }

    const question = String(body.question ?? "").trim().slice(0, MAX_QUESTION);
    const context = String(body.context ?? "").slice(0, MAX_CONTEXT);
    if (!question) {
      return json({ error: "missing question" }, 400);
    }

    const model = env.GEMINI_MODEL || "gemini-3.5-flash";
    const upstream = await fetch(
      `https://generativelanguage.googleapis.com/v1beta/models/${model}:generateContent`,
      {
        method: "POST",
        headers: {
          "content-type": "application/json",
          "x-goog-api-key": env.GEMINI_API_KEY,
        },
        body: JSON.stringify({
          systemInstruction: { parts: [{ text: context }] },
          contents: [{ role: "user", parts: [{ text: question }] }],
          generationConfig: { temperature: 0.35, topP: 0.9 },
        }),
      },
    );

    if (!upstream.ok) {
      return json({ error: "upstream error" }, 502);
    }

    const data = await upstream.json();
    const answer = (data?.candidates?.[0]?.content?.parts ?? [])
      .map((part) => part.text ?? "")
      .join("")
      .trim();

    return json({ answer: answer || null });
  },
};

function json(payload, status = 200) {
  return new Response(JSON.stringify(payload), {
    status,
    headers: { "content-type": "application/json; charset=utf-8" },
  });
}
