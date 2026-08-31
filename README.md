# Zerodha Portfolio Widget

Android home-screen portfolio widget for a personal Zerodha account.

## v0.2.0

- Live Kite holdings through Kite Connect.
- Cached portfolio snapshot so the widget never needs to start with a blank value.
- Asynchronous widget refresh; tapping the widget opens the app.
- Total current value, invested value, overall P&L and equity day P&L.
- Encrypted storage for the API key and access token.
- Coin invested/current totals can be entered manually.
- Coin CSV import with common invested/current-value column names.
- One-tap link to open Coin.
- GitHub Actions Android build.

## Kite authentication

The current app accepts a Kite API key and access token. Zerodha's official authentication flow uses a request token plus an API secret to create an access token, and Zerodha explicitly warns that the API secret must not be embedded in a mobile application. A production-grade version should therefore add a small backend for the token exchange rather than putting the secret in the APK.

Kite access tokens expire at 6 AM the following day, so the app will need a new session after expiry unless a supported long-lived refresh mechanism is available for the account.

## Coin

Zerodha documents viewing mutual-fund investments and details in Coin, including invested amount, current value, P&L, XIRR, NAV and units. The public Kite API does not expose the Coin portfolio as a separate Coin API. This project therefore avoids scraping Coin credentials or private endpoints.

If you export a Coin/portfolio CSV containing columns such as `Invested Amount` and `Current Amount`, use **Import CSV** in the app. The importer aggregates those columns into the widget's Coin total.

## Important limitation

Kite Connect Personal provides portfolio/account APIs but not real-time or historical market data. The current equity value is based on the `last_price` returned by the holdings endpoint. For richer real-time pricing, a market-data source would be required.

## Roadmap

- Backend-assisted Kite login/token exchange.
- More robust background scheduling with WorkManager.
- Per-fund Coin holdings and XIRR.
- Portfolio charts and history.
- Multiple widget sizes/designs.
- Signed release APK/AAB workflow.

## Disclaimer

Personal portfolio viewer only. Not investment advice and not a trading system. Keep Zerodha credentials private and comply with Zerodha/Kite Connect terms.
