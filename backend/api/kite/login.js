import { createTransaction } from "../../lib/auth-store.js";

export default function handler(req, res) {
  const apiKey = process.env.KITE_API_KEY;
  const callbackUrl = process.env.KITE_REDIRECT_URI || process.env.APP_REDIRECT_URI;
  if (!apiKey || !callbackUrl) return res.status(500).json({ error: "Kite login is not configured" });

  const state = createTransaction({ callbackUrl });
  const url = new URL("https://kite.zerodha.com/connect/login");
  url.searchParams.set("v", "3");
  url.searchParams.set("api_key", apiKey);
  url.searchParams.set("state", state);
  res.setHeader("Cache-Control", "no-store");
  res.writeHead(302, { Location: url.toString() });
  res.end();
}
