import crypto from "node:crypto";

export default function handler(req, res) {
  const code = req.query?.code;
  const keyHex = process.env.KITE_CODE_KEY;
  if (!code || !keyHex) return res.status(400).json({ error: "Missing code or server configuration" });
  try {
    const key = Buffer.from(keyHex, "hex");
    if (key.length !== 32) throw new Error("Invalid code key");
    const raw = Buffer.from(code, "base64url");
    const iv = raw.subarray(0, 12);
    const tag = raw.subarray(12, 28);
    const ciphertext = raw.subarray(28);
    const decipher = crypto.createDecipheriv("aes-256-gcm", key, iv);
    decipher.setAuthTag(tag);
    const payload = JSON.parse(Buffer.concat([decipher.update(ciphertext), decipher.final()]).toString("utf8"));
    if (!payload.issuedAt || Date.now() - payload.issuedAt > 60_000) return res.status(410).json({ error: "Login code expired" });
    return res.status(200).json({ api_key: payload.apiKey, access_token: payload.accessToken });
  } catch {
    return res.status(400).json({ error: "Invalid login code" });
  }
}
