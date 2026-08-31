# Zerodha Portfolio Widget

Android home-screen portfolio widget for a personal Zerodha account.

## v0.3.0

- Live Kite holdings through Kite Connect.
- Cache-first widget rendering so the last successful portfolio remains visible.
- Automatic background refresh with WorkManager every 30 minutes when Android permits it.
- Total current value, invested value, overall P&L and equity day P&L.
- Encrypted storage for the API key, access token and cached portfolio.
- Coin invested/current totals can be entered manually.
- Coin CSV import with common invested/current-value column names.
- One-tap link to open Coin.
- Widget tap opens the full app.
- GitHub Actions Android build.

## Kite authentication

The app currently accepts a Kite API key and access token. Zerodha's authentication flow exchanges a short-lived request token for an access token using the app secret, and the API secret must not be embedded in a mobile application. The repository intentionally does not put the secret in the APK.

Zerodha also states that opening the Kite mobile app for third-party authentication is a private flow for exchange-approved partner apps. This project therefore uses the supported public login flow rather than pretending to have partner-only authentication. A future backend can perform the token exchange while keeping the secret server-side.

Kite access tokens are short-lived and require a new login/session when they expire.

## Coin

Zerodha documents viewing mutual-fund investments and details in Coin, including invested amount, current value, P&L, XIRR, NAV and units. The public Kite API does not expose the Coin portfolio as a separate Coin API. This project therefore avoids scraping Coin credentials or private endpoints.

If you export a Coin/portfolio CSV containing columns such as `Invested Amount` and `Current Amount`, use **Import CSV** in the app. The importer aggregates those columns into the widget's Coin total.

## Important limitation

Kite Connect Personal provides portfolio/account APIs but not real-time or historical market data. The current equity value is based on the `last_price` returned by the holdings endpoint. For richer real-time pricing, a market-data source would be required.

## Roadmap

- Backend-assisted Kite login/token exchange.
- Per-fund Coin holdings and XIRR.
- Portfolio charts and history.
- Multiple widget sizes/designs.
- Signed release APK/AAB workflow.

## Disclaimer

Personal portfolio viewer only. Not investment advice and not a trading system. Keep Zerodha credentials private and comply with Zerodha/Kite Connect terms.
