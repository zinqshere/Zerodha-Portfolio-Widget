# Zerodha Portfolio Widget

An Android home-screen widget for a personal Zerodha portfolio.

## Current MVP

- Live Kite holdings through Kite Connect Personal API.
- Calculates equity invested value, current value and P&L from holdings.
- Combines Kite totals with Coin mutual-fund totals stored locally.
- Home-screen widget with current portfolio value and P&L.
- Kite API credentials are encrypted on-device.
- Coin totals are currently entered manually.

## Why Coin is manual for now

Kite Connect exposes supported portfolio APIs for holdings and positions, while Coin does not currently expose an equivalent supported public portfolio API. This project therefore keeps Coin behind a separate data-provider layer so a supported Coin source can be added later without rewriting the widget.

## Kite setup

1. Create a personal Kite Connect app in the Zerodha developer console.
2. Obtain the API key and access token.
3. Enter them in the app. They are stored using Android encrypted preferences.
4. Add the widget to the Android home screen.

Kite Connect Personal provides account access for positions, holdings and funds but does not include real-time or historical market-data APIs. The widget therefore uses the prices available in the holdings response for the current MVP.

## Roadmap

- Proper Kite login/request-token flow.
- Background WorkManager refresh.
- Glance-based adaptive widgets.
- Detailed holdings screen.
- Coin CSV import and/or a supported Coin data source.
- Portfolio history and charts.
- Multiple widget layouts.
- Automated release APK/AAB builds.

## Disclaimer

This is a personal portfolio viewer, not investment advice or a trading system. Keep API credentials private and review Zerodha's API terms before use.
