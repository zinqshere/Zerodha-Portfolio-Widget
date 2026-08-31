# Zerodha Portfolio Widget

Android home-screen portfolio widget for a personal Zerodha account.

## v0.4.0

- Live Kite holdings through Kite Connect.
- Cache-first widget rendering.
- Automatic background refresh with WorkManager every 30 minutes when Android permits it.
- Total current value, invested value, overall P&L and equity day P&L.
- Encrypted local storage for the Kite session and portfolio cache.
- **Backend-assisted Kite login**: the API secret stays server-side.
- Coin invested/current totals can be entered manually or imported from CSV.
- One-tap link to open Coin.
- Widget tap opens the full app.

## Kite login backend

Zerodha's official Kite Connect documentation requires a remote backend for mobile/desktop applications so the `api_secret` is never embedded in the app. The backend in `backend/` implements that handshake: Kite login → request token → server-side checksum/token exchange → encrypted short-lived callback code → Android exchange. citeturn1search0turn1search2

### Deploy the backend

Deploy the `backend/` directory to a Node serverless host such as Vercel. Configure:

- `KITE_API_KEY` — your public Kite API key
- `KITE_API_SECRET` — your private Kite API secret
- `KITE_CODE_KEY` — 64 random hexadecimal characters (32 bytes)
- `APP_REDIRECT_URI` — `zerodhaportfolio://oauth`

In the Kite Connect developer console, set the registered redirect URL to:

`https://YOUR-BACKEND-DOMAIN/api/kite/callback`

Then enter `https://YOUR-BACKEND-DOMAIN` in the Android app and press **Connect Kite**.

The Android app never receives or stores the API secret. The access token is returned through an encrypted, short-lived callback code and stored locally using Android encrypted preferences. Kite access tokens expire at 6 AM the following day, so the user must complete the supported login flow again after expiry. citeturn1search0

> **Important:** Do not commit `.env` files, API secrets, or real access tokens.

## Coin

Zerodha documents mutual-fund portfolio information such as invested amount, current value, P&L, XIRR, NAV and units in Coin. This project deliberately avoids scraping Coin credentials or private endpoints. Coin can currently be represented through manual totals or CSV import.

## Important limitation

Kite Connect Personal provides portfolio/account APIs but does not by itself provide real-time or historical market data for this app. The current equity value uses the `last_price` available from the holdings response.

## Roadmap

- Per-fund Coin holdings and XIRR.
- Portfolio charts and history.
- Multiple widget sizes/designs.
- Signed release APK/AAB workflow.
- More robust one-time backend authorization codes for multi-user deployments.

## Disclaimer

Personal portfolio viewer only. Not investment advice and not a trading system. Keep Zerodha credentials private and comply with Zerodha/Kite Connect terms.
