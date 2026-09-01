# Zerodha Portfolio Widget

An Android home-screen portfolio widget for viewing your **Zerodha Kite equity holdings** and **Coin mutual-fund holdings** in one place.

The app is designed to give you a quick portfolio view without repeatedly opening Zerodha. Your Kite API secret stays on the backend; the Android app only uses the authentication flow and portfolio data it needs.

> **Important:** This project is an independent portfolio viewer and is not affiliated with or endorsed by Zerodha.

---

## What you get

- Zerodha Kite equity portfolio value and returns.
- Coin mutual-fund value and returns when the supported Coin data flow is configured.
- Combined equity + mutual-fund portfolio view.
- Today's P&L where available.
- Last-updated information.
- Secure backend-assisted Kite login.
- Multiple home-screen widgets.
- Independent settings for every widget.
- **Compact** and **Standard** widget layouts.
- **Light / Monet**, **Dark Monet**, and **Pitch Black** widget themes.
- Adjustable widget opacity.
- Optional Today's P&L and equity/mutual-fund breakdown.
- Widget refresh action.
- Live widget preview while configuring a widget.
- Cache-first widget display so the last successful portfolio snapshot can remain visible when a refresh is temporarily unavailable.
- Encrypted local storage for the supported Kite session and portfolio cache.

---

# Setup

There are three things you need:

1. A **Zerodha account**.
2. A **Kite Connect application** with its API key and API secret.
3. A **Vercel backend deployment** that handles the Kite authentication securely.

You do **not** put the Kite API secret into the Android app.

---

# Part 1 — Create your Kite Connect application

## 1. Open the Kite developer portal

Sign in to Zerodha and open the **Kite Connect developer console**.

Create a new Kite Connect application according to your Zerodha/Kite account's available plan and access.

You will receive:

- **API key** — this identifies your Kite application.
- **API secret** — this is private and must never be exposed publicly.

Keep the API secret somewhere secure. You will enter it only as a Vercel environment variable.

## 2. Decide your callback URL

Your authentication flow uses the following Android callback URI:

```text
zerodhaportfolio://oauth
```

This is the URI that returns control from the backend to the Android app after authentication.

Your Kite developer application's registered redirect URL, however, should point to the **backend callback endpoint**, for example:

```text
https://your-project.vercel.app/api/kite/callback
```

Do not register the Android `zerodhaportfolio://oauth` URI as the Kite web redirect if your backend is the component receiving the Kite callback.

---

# Part 2 — Deploy the Vercel backend

The repository contains a `backend/` directory. This is the server-side part of the application that protects your Kite API secret and performs the Kite authentication exchange.

The flow is:

```text
Android app
    │
    │ Connect Kite
    ▼
Vercel backend
    │
    │ OAuth
    ▼
Zerodha Kite
    │
    │ callback
    ▼
Vercel backend
    │
    │ encrypted short-lived code
    ▼
Android app
```

## 1. Create a Vercel account

Create/sign in to your Vercel account and create a new project from this repository.

When configuring the project, make sure the deployment uses the repository's **`backend/` directory** as the backend project/root where required by your Vercel setup.

After deployment, Vercel will give you a public HTTPS address such as:

```text
https://your-project.vercel.app
```

This is your **Auth backend URL**.

## 2. Add the environment variables

Open your Vercel project and go to its **Environment Variables** settings.

Add these variables:

| Variable | What to enter |
|---|---|
| `KITE_API_KEY` | Your Kite Connect API key |
| `KITE_API_SECRET` | Your private Kite Connect API secret |
| `KITE_CODE_KEY` | A 32-byte random encryption key represented by 64 hexadecimal characters |
| `APP_REDIRECT_URI` | `zerodhaportfolio://oauth` |

Example:

```text
KITE_API_KEY=your_api_key
KITE_API_SECRET=your_private_api_secret
KITE_CODE_KEY=64_hexadecimal_characters
APP_REDIRECT_URI=zerodhaportfolio://oauth
```

**Do not use these example values literally.**

### Generate `KITE_CODE_KEY`

You need exactly **64 hexadecimal characters** representing 32 random bytes.

If OpenSSL is available on your computer:

```bash
openssl rand -hex 32
```

Copy the generated value into `KITE_CODE_KEY` in Vercel.

Do not post this key publicly and do not commit it to the repository.

## 3. Redeploy after adding variables

After adding or changing environment variables, redeploy the Vercel project so the running backend receives the new values.

## 4. Register the Vercel callback in Kite

Take your Vercel domain and append the backend callback path:

```text
https://your-project.vercel.app/api/kite/callback
```

Register that **exact URL** as the redirect/callback URL in your Kite Connect application.

For example:

```text
Vercel backend:
https://my-portfolio.vercel.app

Kite callback:
https://my-portfolio.vercel.app/api/kite/callback
```

The callback URL configured in Kite must match the URL your backend expects.

---

# Part 3 — Connect the Android app

## 1. Open the app

Install and open the Zerodha Portfolio Widget app.

## 2. Open Settings

Open the app's **Settings** from the top-right corner.

## 3. Enter your Auth backend URL

In the Kite/Zerodha connection section, enter the Vercel URL **without the callback path**.

For example:

```text
https://my-portfolio.vercel.app
```

Do not enter:

```text
https://my-portfolio.vercel.app/api/kite/callback
```

The app needs the **base backend URL** because it knows which backend authentication endpoints to call.

## 4. Connect Kite

Tap **Connect Kite** / **Connect Zerodha**.

A Zerodha authorization page will open.

Sign in and authorize your Kite application.

After authorization:

1. Kite sends the callback to your Vercel backend.
2. The backend performs the sensitive token/checksum exchange.
3. The backend creates the short-lived encrypted callback code.
4. The Android app receives the callback.
5. The app exchanges the code with the backend.
6. The resulting Kite session is stored using the app's encrypted local storage.
7. The portfolio is refreshed.

The private `KITE_API_SECRET` is never supposed to be stored in the Android APK.

---

# Kite API setup — important details

## API key vs API secret

### API key

The API key identifies your Kite Connect application.

### API secret

The API secret is private. Treat it like a password.

**Never:**

- Put it in Kotlin/Java source code.
- Put it in Android resources.
- Put it in a public `.env` file.
- Put it in the Git repository.
- Put it in screenshots or documentation.
- Enter it into the Android app's backend URL field.

The intended setup is:

```text
KITE_API_SECRET
      │
      ▼
Vercel Environment Variables
      │
      ▼
Backend
```

## Kite login expiry

Kite access tokens are short-lived and normally expire at the broker's daily token-expiry time. When the session expires, complete the supported Kite authorization flow again from the app.

If the app says that the Kite session is no longer valid, reconnect Kite rather than entering your API secret into the Android application.

## Kite portfolio data

The app uses the portfolio/holdings information available through the configured Kite integration. Current portfolio values can therefore depend on the data returned by Kite at refresh time.

---

# Coin mutual-fund setup

The combined portfolio can include your Coin mutual funds when the supported Coin data flow is configured.

The project deliberately does **not** ask you to give the Android app your Coin password or scrape undocumented/private Coin endpoints.

Depending on the current backend/data implementation, Coin information is obtained through the supported synchronization/import flow available to the deployment.

Once Coin data is available, the app can show the combined:

- Equity value
- Mutual-fund value
- Total portfolio value
- Supported P&L/return information

If your Coin values are missing, first check that the backend's Coin data source/synchronization has completed successfully. Changing widget settings cannot create missing Coin data.

---

# Add the home-screen widget

After Kite is connected and portfolio data is available:

1. Long-press an empty area of your Android home screen.
2. Select **Widgets**.
3. Find **Zerodha Portfolio Widget**.
4. Drag it onto the home screen.
5. The **Widget settings** page will open.
6. Use the preview to check the appearance.
7. Choose a layout:
   - **Compact — value + return**
   - **Standard — breakdown**
8. Choose an appearance:
   - **Light / Monet**
   - **Dark Monet**
   - **Pitch black**
9. Adjust opacity if desired.
10. Choose which portfolio information should be visible.
11. Tap **Save widget**.

You can add multiple widgets. Every widget can have its **own layout, theme, opacity and information settings**.

For example:

```text
Widget 1 → Compact + Pitch black
Widget 2 → Standard + Dark Monet
Widget 3 → Standard + Light / Monet
```

Changing one widget does not change the others.

---

# Widget layouts

## Compact

Best for a small space.

It prioritizes:

- Portfolio name
- Total portfolio value
- Overall P&L
- Overall return percentage
- Refresh action

## Standard

Best for a larger widget.

It can show:

- Total portfolio value
- Overall P&L
- Overall return percentage
- Today's P&L
- Equity value and return
- Mutual-fund value and return
- Last-updated information
- Refresh action

---

# Widget appearance

Each widget can use a different appearance.

### Light / Monet

A light Material-style appearance that works well with light wallpapers and Android's dynamic colors where supported.

### Dark Monet

A dark Material-style appearance that uses the device's dynamic color system where supported.

### Pitch black

Uses pure black surfaces and is intended for OLED-friendly viewing.

### Opacity

The widget opacity can be adjusted from the widget settings page.

---

# Widget settings

The widget settings page provides:

### Preview

A live preview shows how the widget will look before you save it.

### Layout

- Compact
- Standard

### Appearance

- Light / Monet
- Dark Monet
- Pitch black

### Information

Depending on the current widget configuration, you can enable or disable:

- Today's P&L
- Equity + mutual-fund breakdown

### Per-widget configuration

Settings belong to the individual widget. Multiple widgets can therefore have different configurations while using the same portfolio account.

---

# Refreshing your portfolio

The app and widget can refresh the portfolio using the configured backend/Kite session.

The widget also keeps the last successful snapshot so that temporary network/backend restrictions do not necessarily leave the widget empty.

Android may delay background work because of battery optimization and other system restrictions. A background refresh is therefore not guaranteed to happen at an exact minute.

If the displayed data is old, open the app and use the available **Refresh** action. If your Kite session has expired, reconnect Kite first.

---

# Troubleshooting

## “Can't load widget”

If Android displays **Can't load widget**:

1. Remove the widget from the home screen.
2. Make sure the app itself opens normally.
3. Install the latest available app version.
4. Add the widget again.
5. Complete its configuration and tap **Save widget**.

If the widget still cannot load, restart the Android launcher/device and try again.

## “Kite connection failed”

Check all of these:

- The Vercel backend URL is correct.
- You entered the **base URL**, not `/api/kite/callback`, in the Android app.
- The Vercel deployment is running.
- `KITE_API_KEY` is correct.
- `KITE_API_SECRET` is correct.
- `KITE_CODE_KEY` is present and contains 64 hexadecimal characters.
- `APP_REDIRECT_URI` is exactly `zerodhaportfolio://oauth`.
- The Kite developer application's callback is exactly:

```text
https://YOUR-DOMAIN/api/kite/callback
```

- The callback URL points to the same Vercel deployment entered in the app.
- You have completed the Zerodha authorization flow.

## “Invalid redirect URI” from Zerodha

The most common cause is a mismatch between the callback URL registered in Kite and the actual Vercel backend URL.

For a backend hosted at:

```text
https://my-portfolio.vercel.app
```

register:

```text
https://my-portfolio.vercel.app/api/kite/callback
```

Do not add a trailing slash unless the backend and Kite configuration both explicitly use it.

## Coin mutual funds are not showing

Verify that the supported Coin synchronization/import flow has completed and that your backend deployment has access to the required Coin data source.

Kite equity authentication alone does not automatically guarantee that every Coin mutual-fund field is available.

## Widget shows old data

Try a manual refresh from the app or widget.

If the Kite token has expired, reconnect Kite.

Also remember that Android can defer background refreshes.

## Widget looks different from the preview

The Android launcher controls the physical widget dimensions and can apply its own padding or corner treatment. Try resizing the widget and re-saving its configuration.

---

# Security checklist

Before using the app with a real account, verify:

- [ ] `KITE_API_SECRET` exists only in the secure Vercel environment.
- [ ] `KITE_CODE_KEY` exists only in the secure Vercel environment.
- [ ] No real Kite credentials are committed to the repository.
- [ ] The backend is accessed through HTTPS.
- [ ] The Kite callback URL points to your own backend deployment.
- [ ] The Android app contains only the backend URL and does not contain your private Kite API secret.
- [ ] You do not share access tokens, callback codes or screenshots containing sensitive account information.

If you accidentally expose your Kite API secret, rotate/revoke it through the appropriate Zerodha/Kite developer controls immediately.

---

# Privacy and data handling

The app needs access to portfolio information in order to display it.

The intended architecture keeps the private Kite API secret on the backend and stores the supported Android session/cache using encrypted local storage.

Use a Vercel account and backend deployment that you control or trust. Do not enter your financial credentials into an unknown backend URL.

---

# Disclaimer

This application is a portfolio-viewing tool. It is **not investment advice**, a trading system, or a recommendation to buy or sell securities.

Zerodha, Kite and Coin are trademarks/brands of their respective owners. This project is independent and is not affiliated with or endorsed by Zerodha unless explicitly stated by the project owner.

Always verify important portfolio information directly with Zerodha before making financial decisions.
