export default function handler(req, res) {
  const apiKey = process.env.KITE_API_KEY;
  if (!apiKey) return res.status(500).json({ error: "KITE_API_KEY is not configured" });
  const url = new URL("https://kite.zerodha.com/connect/login");
  url.searchParams.set("v", "3");
  url.searchParams.set("api_key", apiKey);
  res.writeHead(302, { Location: url.toString() });
  res.end();
}
