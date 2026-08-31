# Zerodha Portfolio Widget

A personal Android portfolio viewer and home-screen widget for **Zerodha Kite equity holdings** and **Coin mutual-fund holdings**.

The project is designed for quick portfolio checks without opening a brokerage app. The Android app handles the UI and widget configuration, while the optional backend handles the sensitive Kite authentication flow so the Kite API secret is not embedded in the APK.

> **Project status:** Active development. Zerodha/Kite access and Coin functionality depend on the backend configuration and credentials available to the deployment.

---

## Contents

- [Features](#features)
- [How it works](#how-it-works)
- [Requirements](#requirements)
- [User setup](#user-setup)
- [Kite backend setup](#kite-backend-setup)
- [Coin setup](#coin-setup)
- [Home-screen widgets](#home-screen-widgets)
- [Widget settings](#widget-settings)
- [Building the Android app](#building-the-android-app)
- [Release and update compatibility](#release-and-update-compatibility)
- [Security](#security)
- [Troubleshooting](#troubleshooting)
- [Development](#development)
- [Disclaimer](#disclaimer)

---

## Features

### Portfolio

- Live Kite equity holdings through Kite Connect.
- Portfolio current value, invested value and overall P&L.
- Equity day P&L when supplied by the Kite holdings data.
- Combined portfolio presentation for equity and mutual funds.
- Last-updated information so stale data is visible.
- Cache-first rendering so the widget can display the last successful portfolio snapshot while a refresh is unavailable.
- Encrypted local storage for the Kite session and cached portfolio data.

### Zerodha authentication

- Backend-assisted Kite login.
- Kite API secret stays on the server instead of being packaged into the Android application.
- Short-lived callback-code exchange between the backend and Android app.
- The backend performs the server-side Kite token/checksum exchange.

### Coin mutual funds

The app supports the project's Coin portfolio flow for bringing mutual-fund totals into the combined portfolio. The exact Coin source depends on the current backend implementation/configuration.

The project does **not** scrape Coin passwords or rely on undocumented private Coin endpoints. Where the current implementation requires an import/manual source, use the supported flow exposed by the app/backend.

### Home-screen widget

- Multiple Zerodha Portfolio widgets can be placed on the launcher.
- Each widget has its **own configuration**.
- Compact and Standard layouts.
- Light / Monet, Dark Monet and Pitch Black appearances.
- Adjustable widget opacity.
- Optional Today's P&L.
- Optional equity + mutual-fund breakdown.
- Direct refresh action.
- Tap interaction to open the app.
- Responsive rendering for different launcher widget dimensions.
- Configuration screen with a live preview.

### UI

- Material 3 based Android interface.
- Widget configuration is vertically scrollable on small screens.
- The configuration screen follows the app/device light or dark appearance.
- Status and navigation bars follow the active appearance rather than being permanently forced to light or dark.
- The main app keeps widget configuration out of the primary portfolio screen.

---

## How it works

The high-level architecture is:

```text
                         HTTPS
Android app  ------------------------------>  Vercel / backend
     |                                             |
     |                                             | Kite OAuth/API
     |                                             v
     |                                         Zerodha Kite
     |
     +---- Home-screen widget
```

The Android app does not need the private Kite API secret. Instead:

1. The user starts **Connect Kite** in the Android app.
2. The app sends the user through the configured backend.
3. The backend redirects to Zerodha/Kite for authorization.
4. Kite returns an authorization/request token to the backend callback.
5. The backend performs the server-side token/checksum exchange.
6. The backend returns a short-lived encrypted callback code to the Android app.
7. The Android app exchanges that code and stores the resulting session securely.
8. Portfolio data is fetched and cached for the app/widget.

Kite access tokens expire at approximately 6 AM the following day, so a new supported authorization flow may be required after expiry.

---

## Requirements

### For normal use

- Android device with home-screen widget support.
- Zerodha account.
- Kite Connect application/API access for the configured integration.
- A deployed and working backend URL.
- Coin data source/configuration if mutual-fund data is required.

### For development

- Git.
- Android Studio.
- Android SDK compatible with the repository's Gradle configuration.
- The JDK version required by the repository's Gradle/CI configuration.
- A Zerodha Kite Connect developer application.
- Vercel or another compatible Node/serverless host for the backend.

Use the versions declared by the project and GitHub Actions rather than arbitrarily changing Gradle, Kotlin, Android Gradle Plugin or JDK versions.

---

# User setup

## 1. Install the Android app

Install a successful release APK produced by the repository's GitHub Actions workflow, or build the application locally.

For a release APK, always use a build that completed successfully in CI. Do not install an APK from a failed workflow run.

## 2. Deploy/configure the backend

The Android app needs the public HTTPS URL of the authentication backend.

In the app's Zerodha/Kite section, enter the backend URL and use **Connect Kite**.

The backend URL should look like:

```text
https://your-project.vercel.app
```

Do not put `KITE_API_SECRET` or any other private credential into the Android app.

## 3. Connect Kite

1. Open the app.
2. Open the Kite connection/settings area.
3. Enter the configured **Auth backend URL**.
4. Tap **Connect Kite**.
5. Complete the Zerodha authorization page.
6. Return to the app through the configured callback flow.
7. Wait for the portfolio to load.

If authorization succeeds, the Android app stores the resulting session using encrypted local storage.

## 4. Configure Coin

Use the Coin flow supported by the current backend/app deployment to make your mutual-fund data available.

The combined portfolio can then present:

- Equity value
- Mutual-fund value
- Combined total
- Corresponding returns where supported by the current data source

Do not upload credentials or private account data to an untrusted third-party service.

## 5. Add a widget

From your Android launcher:

1. Long-press an empty area of the home screen.
2. Tap **Widgets**.
3. Find **Zerodha Portfolio Widget**.
4. Drag it onto the home screen.
5. Complete the widget settings screen.
6. Choose **Compact** or **Standard**.
7. Choose the appearance.
8. Set opacity if desired.
9. Choose whether to show Today's P&L.
10. Choose whether to show the equity + mutual-fund breakdown.
11. Tap **Save widget**.

Repeat the process to create additional widgets. Settings are stored independently for each widget.

---

# Kite backend setup

The backend lives in the repository's `backend/` directory and is intended to be deployed to a Node/serverless platform such as Vercel.

## 1. Create a Kite Connect application

Create/configure your application in the Zerodha Kite developer portal and obtain the application API key and API secret.

Keep the API secret private.

## 2. Deploy `backend/`

Deploy the repository's `backend/` directory to Vercel or another compatible Node/serverless host.

Configure the required environment variables in the hosting provider's secret/environment-variable settings.

The current backend expects the following variables:

| Variable | Purpose |
|---|---|
| `KITE_API_KEY` | Public Kite API key used by the backend |
| `KITE_API_SECRET` | Private Kite API secret; server-side only |
| `KITE_CODE_KEY` | 64 hexadecimal characters used as the 32-byte encryption key for callback codes |
| `APP_REDIRECT_URI` | Android callback URI used by the authentication flow |

Example:

```text
KITE_API_KEY=your_kite_api_key
KITE_API_SECRET=your_kite_api_secret
KITE_CODE_KEY=<64 random hexadecimal characters>
APP_REDIRECT_URI=zerodhaportfolio://oauth
```

**Do not copy these example values into production.**

## 3. Generate `KITE_CODE_KEY`

Generate 32 random bytes and encode them as 64 hexadecimal characters.

For example, on a machine with OpenSSL:

```bash
openssl rand -hex 32
```

Put the result into the Vercel environment variable `KITE_CODE_KEY`.

## 4. Configure the Kite redirect URL

If the deployed backend is:

```text
https://your-project.vercel.app
```

then the Kite developer application should use the backend's callback endpoint:

```text
https://your-project.vercel.app/api/kite/callback
```

The exact redirect URL must match the backend configuration.

## 5. Configure the Android app

Enter the backend's public base URL in the Android app:

```text
https://your-project.vercel.app
```

Then press **Connect Kite**.

### Security model

The Android APK contains the public API key only when required by the integration; the private API secret remains in the backend environment. The backend performs the sensitive Kite exchange.

---

# Coin setup

Coin mutual-fund data is separate from the Kite equity holdings API.

The project intentionally avoids storing or scraping Coin login credentials and private undocumented endpoints.

Depending on the current backend implementation, Coin data may be supplied through the supported import/synchronization flow. The resulting mutual-fund totals can then be included in the combined portfolio and widget breakdown.

If Coin values are missing, verify the backend's Coin configuration and the data source used by the deployment before changing Android widget settings.

---

# Home-screen widgets

## Supported layouts

### Compact

Designed for a small widget footprint. It prioritizes the most important information:

- Zerodha Portfolio label
- Total portfolio value
- Overall P&L
- Overall return percentage
- Refresh action

### Standard

Designed for a larger widget footprint. It can show:

- Total portfolio value
- Overall P&L
- Overall return percentage
- Today's P&L
- Equity value and return
- Mutual-fund value and return
- Last-updated information
- Refresh action

The current settings page intentionally exposes only **Compact** and **Standard**. Older development versions had additional layout values; these are migrated to a supported layout rather than shown to users.

## Multiple widgets

Each Android widget instance has an independent configuration.

For example:

```text
Widget 1 -> Compact + Pitch Black
Widget 2 -> Standard + Dark Monet
Widget 3 -> Standard + Light / Monet
```

Changing Widget 1 does not automatically change Widget 2 or Widget 3.

---

# Widget settings

The widget configuration page contains:

### Layout

- Compact — value + return
- Standard — breakdown

### Appearance

- Light / Monet
- Dark Monet
- Pitch black

### Opacity

Adjust the widget surface opacity from 20% to 100%.

### Information

- Today's P&L — show/hide day performance.
- Equity + mutual-fund breakdown — show/hide the two portfolio components.

The page contains a live widget preview and is vertically scrollable so all settings remain accessible on smaller phones.

The **Save widget** action remains accessible at the bottom of the configuration screen.

---

# Building the Android app

## Clone the repository

```bash
git clone https://github.com/zinqshere/Zerodha-Portfolio-Widget.git
cd Zerodha-Portfolio-Widget
```

## Open in Android Studio

Open the repository root in Android Studio and allow Gradle to synchronize.

Do not commit generated build output, local environment files, signing keys or secrets.

## Build with Gradle

Use the Gradle wrapper included in the repository.

For a standard release task:

```bash
./gradlew assembleRelease
```

If the repository's GitHub Actions workflow uses a module-specific task, use that exact task when reproducing CI locally.

For a debug build, use the debug task defined by the project's Gradle modules.

## GitHub Actions

The repository uses GitHub Actions to validate and build Android releases.

A release should be considered ready only when the workflow:

1. Compiles successfully.
2. Passes the configured checks.
3. Validates the signing configuration.
4. Produces the release APK artifact.
5. Completes any APK/signature verification steps configured by CI.

---

# Release and update compatibility

If an existing installation should update without requiring uninstall/reinstall, Android must recognize the new APK as the same application.

Keep all of the following stable:

- **Application ID / package name**
- **Release signing key**

Also ensure that the new release has a higher Android `versionCode` than the installed version.

Changing the application ID or signing key can cause Android to treat the APK as a different application and can require uninstalling the previous installation.

### Signing secrets

Never commit the following to GitHub:

- Keystore files
- Keystore passwords
- Key aliases and private signing material when they are intended to remain secret
- Kite API secrets
- OAuth access tokens
- Production `.env` files

Use GitHub Actions secrets for CI signing credentials and Vercel environment variables for backend credentials.

---

# Security

This application handles financial-account information. Treat all portfolio data and credentials as sensitive.

### Never commit

```text
.env
.env.*
*.jks
*.keystore
KITE_API_SECRET
access tokens
real account credentials
```

### Backend rules

- Keep `KITE_API_SECRET` server-side.
- Use HTTPS in production.
- Use a strong random `KITE_CODE_KEY`.
- Do not log access tokens or API secrets.
- Do not expose production environment variables to the Android client.

### Android rules

- Do not hard-code private credentials in Kotlin/Java/resources.
- Keep the local Kite session in the project's encrypted storage mechanism.
- Avoid logging personal portfolio information unnecessarily.

---

# Troubleshooting

## “Can't load widget”

If Android shows **Can't load widget**:

1. Remove the existing widget from the home screen.
2. Install the latest **successful** release APK.
3. Restart the launcher/device if necessary.
4. Add the widget again.
5. Complete its configuration.

If the error continues, inspect the Android launcher exception and the GitHub Actions build logs. Widget failures are commonly caused by unsupported `RemoteViews` operations, missing resources, invalid widget-provider metadata, or a build that does not contain the latest widget code.

## Widget shows old settings

Each physical widget has its own saved configuration. Reconfigure that specific widget or remove and add it again.

## Kite login does not work

Check:

- The backend URL is correct.
- The backend is deployed and healthy.
- `KITE_API_KEY` and `KITE_API_SECRET` are configured on the backend.
- `KITE_CODE_KEY` is present and valid.
- `APP_REDIRECT_URI` matches the Android callback configuration.
- The Kite developer application's registered callback matches the backend callback URL exactly.
- The authorization flow is using HTTPS in production.

## Portfolio data is stale

The widget uses cached data when necessary and background refresh is subject to Android's background-execution rules. Open the app and trigger a supported refresh/authentication flow if the cache is old.

The project currently uses WorkManager for periodic background refresh; Android may defer scheduled work depending on battery, background and system conditions.

## Coin data is missing

Verify that the supported Coin import/synchronization flow has completed successfully and that the backend is configured for the Coin data source used by your deployment.

Changing the widget layout cannot create missing portfolio data.

## Release APK will not update an existing installation

Check:

- Same application ID/package name.
- Same release signing key.
- Higher `versionCode`.
- Correct release variant.
- CI is using the intended signing secrets.

If the signing key has changed, Android will normally reject the APK as an update.

---

# Development

## Recommended workflow

1. Create a feature branch.
2. Make a focused change.
3. Run the relevant Gradle checks/build.
4. Test UI changes on an Android device or emulator.
5. For widget changes, add, resize, refresh and remove/re-add a real widget.
6. Verify both light and dark appearance.
7. Verify a fresh install and an in-place update when changing release/signing configuration.
8. Open a pull request or merge the tested change into `main`.

## Widget testing checklist

Whenever the widget implementation changes, test:

- Compact layout.
- Standard layout.
- Light / Monet appearance.
- Dark Monet appearance.
- Pitch black appearance.
- Different opacity values.
- Today's P&L enabled and disabled.
- Equity/mutual-fund breakdown enabled and disabled.
- Multiple widgets with different configurations.
- Widget refresh.
- Widget resizing.
- Removing and re-adding the widget.
- App update without uninstalling.
- Launcher behavior after device restart.

---

# Project principles

- **Glanceable:** important portfolio numbers should be readable immediately.
- **Informative:** show useful value, return and breakdown information without opening the full app.
- **Up-to-date:** make the last successful update visible.
- **Consistent:** widget styling follows the selected appearance.
- **Interactive:** refresh and useful navigation actions should remain simple.
- **Secure:** private Zerodha credentials belong on the backend, not inside the APK.
- **Update-safe:** releases should preserve the application identity and signing key so users can update normally.

---

# Roadmap

Potential future improvements include:

- More detailed per-fund Coin holdings.
- XIRR and fund-level performance.
- Portfolio history and charts.
- Additional widget sizes/layout refinements.
- More robust multi-user backend authorization.
- Further launcher-specific widget compatibility testing.

---

# License

Check the repository for a `LICENSE` file for the applicable license. If no license file is present, the project should be treated as **all rights reserved** until an explicit license is added.

---

# Disclaimer

This is an independent personal portfolio-viewing project and is **not affiliated with, sponsored by, or endorsed by Zerodha**.

Zerodha, Kite and Coin are trademarks of their respective owners.

This application is a portfolio viewer, not a trading system or investment-advisory service. Portfolio values and returns depend on the data supplied by the configured Zerodha/backend integration. Do not use this application as the sole source for investment or trading decisions.
