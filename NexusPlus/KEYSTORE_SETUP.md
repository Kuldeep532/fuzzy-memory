# Keystore Setup Guide — Nexus Plus

> **Security rule: The keystore file is NEVER stored in the repository.**  
> It is generated locally, encoded, and stored in GitHub Secrets only.

---

## Step 1 — Generate a release keystore (one time only)

Run this on your local machine (NOT in CI):

```bash
keytool -genkey -v \
  -keystore nexus-release.jks \
  -alias nexus-key \
  -keyalg RSA \
  -keysize 4096 \
  -validity 10000 \
  -storepass YOUR_STORE_PASSWORD \
  -keypass  YOUR_KEY_PASSWORD \
  -dname "CN=Nexus Wave Technologies, OU=Android, O=NexusWaveTech, L=Your City, ST=Your State, C=IN"
```

Store `nexus-release.jks` **securely offline** (password manager, encrypted drive).  
**Never push it to git.**

---

## Step 2 — Encode the keystore as Base64

```bash
# Linux / macOS
base64 -w 0 nexus-release.jks > nexus-release-encoded.txt

# Windows (PowerShell)
[Convert]::ToBase64String([IO.File]::ReadAllBytes("nexus-release.jks")) | Out-File -NoNewline nexus-release-encoded.txt
```

---

## Step 3 — Add the four GitHub Secrets

Go to: **GitHub repo → Settings → Secrets and variables → Actions → New repository secret**

| Secret name | Value |
|---|---|
| `ENCODED_PLAY_STORE_KEYSTORE` | Contents of `nexus-release-encoded.txt` |
| `KEY_STORE_PASSWORD` | Your keystore password |
| `KEY_ALIAS` | `nexus-key` (or whatever alias you chose) |
| `KEY_PASSWORD` | Your key password |

After adding, **delete `nexus-release-encoded.txt`** from your local machine.

---

## How the CI pipeline uses it

The `build-release` job in `.github/workflows/android.yml`:

1. Decodes `secrets.ENCODED_PLAY_STORE_KEYSTORE` → writes to `/tmp/nexus-release.jks` (runner RAM disk only)
2. Passes keystore path + passwords to `./gradlew assembleRelease` via `-P` flags
3. **Immediately calls `shred -u`** to securely wipe the file from the runner (runs even if the build fails via `if: always()`)
4. Uploads the signed APK as a GitHub Actions artefact

The keystore **never touches the repository** and **never appears in build logs** (secrets are masked by GitHub).

---

## Verification checklist

- [ ] `nexus-release.jks` is listed in `.gitignore`
- [ ] `git status` shows no `.jks`, `.keystore`, or `.p12` files
- [ ] All four secrets are set in GitHub → Settings → Secrets
- [ ] CI `build-release` job produces `app-release.apk` (not unsigned)
- [ ] Keystore backup stored in a password manager (offline)
