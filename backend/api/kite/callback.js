import crypto from "node:crypto";

export default async function handler(req, res) {
  const requestToken = req.query?.request_token;
  const apiKey = process.env.KITE_API_KEY;
  const apiSecret = process.env.KITE_API_SECRET;
  const appRedirect = process.env.APP_REDIRECT_URI || "zerodhaportfolio://oauth";

  if (!requestToken) return res.status(400).send("Missing request_token");
  if (!apiKey || !apiSecret) return res.status(500).send("Kite backend credentials are not configured");

  const checksum = crypto.createHash("sha256")
    .update(apiKey + requestToken + apiSecret)
    .digest("hex");

  const body = new URLSearchParams({ api_key: apiKey, request_token: requestToken, checksum });
  const response = await fetch("https://api.kite.trade/session/token", {
    method: "POST",
    headers: { "X-Kite-Version": "3", "Content-Type": "application/x-www-form-urlencoded" },
    body
  });

  const data = await response.json();
  if (!response.ok || data.status !== "success" || !data.data?.access_token) {
    return res.status(502).json({ error: "Kite token exchange failed", details: data.message || "Unknown error" });
  }

  const redirect = new URL(appRedirect);
  redirect.searchParams.set("access_token", data.data.access_token);
  redirect.searchParams.set("api_key", apiKey);
  res.writeHead(302, { Location: redirect.toString() });
  res.end();
}
