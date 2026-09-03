import crypto from "node:crypto";

const TTL_MS = 2 * 60_000;

function getKey() {
  const keyHex = process.env.KITE_CODE_KEY;
  if (!keyHex) throw new Error("KITE_CODE_KEY is not configured");
  const key = Buffer.from(keyHex, "hex");
  if (key.length !== 32) throw new Error("KITE_CODE_KEY must be 32 bytes / 64 hex characters");
  return key;
}

function encode(value) {
  return Buffer.from(value, "utf8").toString("base64url");
}

function decode(value) {
  return Buffer.from(value, "base64url").toString("utf8");
}

export function createTransaction(payload) {
  const now = Date.now();
  const body = encode(JSON.stringify({
    ...payload,
    nonce: crypto.randomBytes(16).toString("base64url"),
    createdAt: now,
    expiresAt: now + TTL_MS
  }));
  const signature = crypto.createHmac("sha256", getKey()).update(body).digest("base64url");
  return `${body}.${signature}`;
}

export function consumeTransaction(token) {
  try {
    const separator = token.lastIndexOf(".");
    if (separator <= 0) return null;

    const body = token.slice(0, separator);
    const providedSignature = token.slice(separator + 1);
    const expectedSignature = crypto.createHmac("sha256", getKey()).update(body).digest("base64url");
    const provided = Buffer.from(providedSignature, "base64url");
    const expected = Buffer.from(expectedSignature, "base64url");
    if (provided.length !== expected.length || !crypto.timingSafeEqual(provided, expected)) return null;

    const value = JSON.parse(decode(body));
    if (!value.expiresAt || value.expiresAt <= Date.now()) return null;

    const { nonce, createdAt, expiresAt, ...payload } = value;
    if (!nonce || !createdAt || !expiresAt) return null;
    return payload;
  } catch {
    return null;
  }
}
