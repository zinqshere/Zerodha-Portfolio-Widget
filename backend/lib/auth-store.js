import crypto from "node:crypto";

const transactions = new Map();
const TTL_MS = 2 * 60_000;

function cleanup(now = Date.now()) {
  for (const [id, value] of transactions) {
    if (value.expiresAt <= now) transactions.delete(id);
  }
}

export function createTransaction(payload) {
  const id = crypto.randomBytes(32).toString("base64url");
  const now = Date.now();
  cleanup(now);
  transactions.set(id, { ...payload, createdAt: now, expiresAt: now + TTL_MS });
  return id;
}

export function consumeTransaction(id) {
  const now = Date.now();
  cleanup(now);
  const value = transactions.get(id);
  if (!value || value.expiresAt <= now) return null;
  transactions.delete(id);
  return value;
}
