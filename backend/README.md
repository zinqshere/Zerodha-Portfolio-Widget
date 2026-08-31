# Kite auth backend

This backend keeps `KITE_API_SECRET` off the Android device and performs the supported Kite request-token → access-token exchange.

## Deploy

Deploy the `backend` directory as a Vercel project (or equivalent Node serverless host). Configure these environment variables:

- `KITE_API_KEY` — public Kite API key
- `KITE_API_SECRET` — private Kite API secret
- `APP_REDIRECT_URI` — `zerodhaportfolio://oauth`

In the Kite Connect developer console, set the app's registered redirect URL to:

`https://YOUR-BACKEND-DOMAIN/api/kite/callback`

The app starts login at `/api/kite/login`. Kite redirects to `/api/kite/callback`; the backend computes the SHA-256 checksum and exchanges the one-time request token at `/session/token`. It then redirects to the Android app with the short-lived access token.

## Security

Never commit the API secret. Never put it in the Android project. The access token is returned only after the user completes the official Kite login. Kite states that the access token expires at 6 AM the next day and that the user must go through the login flow again when it expires.

For a single-user personal app this is intentionally minimal. A multi-user deployment should add state binding, one-time authorization codes, rate limiting, audit logging, and server-side token storage rather than returning the access token in a custom URI.
