# Persistent Android release signing

Android only allows an update to install when the new APK is signed with the same signing certificate as the installed APK. The CI workflow previously produced a debug APK with the runner's temporary debug keystore, so different GitHub Actions runs could have different signing certificates.

The project now uses one persistent release keystore for every installable APK. The private keystore is supplied to GitHub Actions through repository secrets and is never committed to the repository.

## One-time setup

Create a release keystore locally:

```bash
keytool -genkeypair -v \
  -keystore release.keystore \
  -alias zerodha-portfolio \
  -keyalg RSA \
  -keysize 4096 \
  -validity 10000
```

Convert it to a single-line Base64 value:

```bash
base64 -w 0 release.keystore
```

Add these four **repository Actions secrets** in GitHub → Settings → Secrets and variables → Actions:

- `RELEASE_KEYSTORE_BASE64` — the Base64 output above
- `RELEASE_KEYSTORE_PASSWORD` — keystore password
- `RELEASE_KEY_ALIAS` — `zerodha-portfolio` (or the alias you selected)
- `RELEASE_KEY_PASSWORD` — key password

Do not commit the keystore or its passwords.

## Important migration note

The signing key used by APKs already installed on a device cannot be recovered from the APK. If the currently installed APK was signed with a temporary CI/debug key and that private key was not preserved, Android will require a **one-time uninstall** before installing the first APK signed with the new persistent release key.

After that migration, all subsequent release APKs built by this workflow use the same certificate and can be installed as normal updates, preserving app data and settings.
