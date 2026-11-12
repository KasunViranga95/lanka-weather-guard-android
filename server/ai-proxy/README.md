# AI helper proxy

The app's "Ask about the weather" helper uses Google Gemini. A Gemini API key must never be built into the app, because anyone can extract it from an APK. This small Cloudflare Worker holds the key on the server instead. The app sends the question to the Worker, and the Worker calls Gemini.

## Deploy (free Cloudflare account)

1. Install Node.js, then run these from this folder:

   ```bash
   npx wrangler login
   npx wrangler deploy
   npx wrangler secret put GEMINI_API_KEY
   ```

   Paste your Gemini key when asked. `deploy` prints the Worker's address, for example `https://lanka-weather-ai.<your-account>.workers.dev`.

2. In the project root, create `.env` (copy `.env.example`) and set:

   ```
   AI_PROXY_URL=https://lanka-weather-ai.<your-account>.workers.dev
   ```

3. Rebuild the app. **Ask about the weather** now appears in the ⋮ menu.

## Limit costs

In the Cloudflare dashboard, add a rate-limiting rule for the Worker's route (for example 20 requests per minute per IP). Also set a budget alert on your Google AI Studio or Google Cloud project.

Without `AI_PROXY_URL`, release builds hide the AI helper. Debug builds can also use `GEMINI_API_KEY` from `.env` directly for testing; release builds never include it.
