import crypto from "node:crypto";

function encryptCode(payload, keyHex) {
  const key = Buffer.from(keyHex, "hex");
  if (key.length !== 32) throw new Error("KITE_CODE_KEY must be 32 bytes / 64 hex characters");
  const iv = crypto.randomBytes(12);
  const cipher = crypto.createCipheriv("aes-256-gcm", key, iv);
  const ciphertext = Buffer.concat([cipher.update(JSON.stringify(payload), "utf8"), cipher.final()]);
  const tag = cipher.getAuthTag();
  return Buffer.concat([iv, tag, ciphertext]).toString("base64url");
}

export default async function handler(req, res) {
  const requestToken = req.query?.request_token;
  const apiKey = process.env.KITE_API_KEY;
  const apiSecret = process.env.KITE_API_SECRET;
  const codeKey = process.env.KITE_CODE_KEY;
  const appRedirect = process.env.APP_REDIRECT_URI || "zerodhaportfolio://oauth";

  if (!requestToken) return res.status(400).send("Missing request_token");
  if (!apiKey || !apiSecret || !codeKey) return res.status(500).send("Kite backend credentials are not configured");

  const checksum = crypto.createHash("sha256").update(apiKey + requestToken + apiSecret).digest("hex");
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

  const code = encryptCode({ apiKey, accessToken: data.data.access_token, issuedAt: Date.now() }, codeKey);
  const redirect = new URL(appRedirect);
  redirect.searchParams.set("code", code);
  res.writeHead(302, { Location: redirect.toString() });
  res.end();
}
