import { createTransaction } from "../../lib/auth-store.js";

export default function handler(req, res) {
  const apiKey = process.env.KITE_API_KEY;
  const callbackUrl = process.env.KITE_REDIRECT_URI || process.env.APP_REDIRECT_URI;
  const codeKey = process.env.KITE_CODE_KEY;
  if (!apiKey || !callbackUrl || !codeKey) {
    return res.status(500).json({ error: "Kite login is not configured" });
  }

  const state = createTransaction({ callbackUrl });
  const url = new URL("https://kite.zerodha.com/connect/login");
  url.searchParams.set("v", "3");
  url.searchParams.set("api_key", apiKey);
  // Kite returns redirect_params to the configured callback. Use it instead
  // of a custom top-level `state` parameter, which Kite does not echo back.
  url.searchParams.set("redirect_params", new URLSearchParams({ state }).toString());
  res.setHeader("Cache-Control", "no-store");
  res.writeHead(302, { Location: url.toString() });
  res.end();
}
