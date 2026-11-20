# 🔐 PasswordKey (HEXA)

A simple, offline Android app to keep your account passwords in one place, with no ads, no accounts, and no internet.

I built this because I was frustrated with Play Store password managers full of ads, data collection, and complicated setups. This is my own minimal version, made for private, personal use.

[![Latest release](https://img.shields.io/github/v/release/sonaaar9n/PasswordKeyHEXA)](https://github.com/sonaaar9n/PasswordKeyHEXA/releases/latest)
![Platform](https://img.shields.io/badge/platform-Android-green)
![Language](https://img.shields.io/badge/language-Kotlin-purple)

## ✨ Features

- Store passwords for your social media and other accounts
- View and manage each account's details
- 100% offline: the app has **no internet permission**
- No ads, no trackers, no sign-up
- Lightweight (~2.3 MB)

## 📲 Download

1. Go to the [**latest release**](https://github.com/sonaaar9n/PasswordKeyHEXA/releases/latest).
2. Download `app-debug.apk`.
3. Open it on your phone. If asked, allow **Install from unknown sources** for your browser or file manager.

## 📸 Screenshots
No need to show you tutorial for this app. 
## 🛡️ Security: please read

This app is **personal-use by design**. It deliberately has no encryption layer. Your passwords are stored on your phone, and protection depends on:

- your phone's **screen lock**, and
- an optional **app lock** (I use my phone's built-in app lock as a second layer).

What this means:

## 🛡️ Security

Personal-use app by design: fully offline, with no internet permission encryption as you want.

**Your data in the app**
- Passwords are stored on your phone only, in the app's private storage. The app's own database is **not encrypted**, so there is no master password to forget and you can never get locked out of your own data.
- It is protected by your phone's screen lock and app lock, so keep those turned on.
- Updating the app keeps all your saved passwords.

**Encrypted backup (.pkbackup)**
- Backups can be locked with a 6-digit PIN (AES-256-GCM). Without the PIN the file cannot be opened.
- If you forget the PIN, the backup cannot be recovered.
- A 6-digit PIN stops casual access, but someone who gets the file and has time and a computer could still try all combinations. Use a PIN you don't use elsewhere and keep the file safe.

**Importing a backup**
- Importing never overwrites your newer data. A password or username from a backup is imported only if its date is newer than what is already saved in the app. Older or equal entries are skipped, so importing an old backup by mistake is safe.
- You always get a preview of what will change before anything is imported.

**Excel / PDF exports are not encrypted.** Anyone who gets those files can read them, so keep them safe or delete them after use. Use the encrypted .pkbackup option when you want a protected copy.

If you need strong protection, use a well-audited password manager. If you want something simple and private for yourself, this might be enough for you. 

## 🔒 Permissions

| Permission | Notes |
It has excel/pdf format export for all the passwords to keep in single file. 
## 🛠️ Build from source

1. Clone the repo:
   ```bash
   git clone https://github.com/sonaaar9n/PasswordKeyHEXA.git
   ```
2. Open the folder in **Android Studio**.
3. Let Gradle sync, then click **Run ▶** or use **Build → Build APK(s)**.

**Stack:** Kotlin, Gradle, package `com.mykey.passwords`.

## 🗺️ Ideas for the future

- [ ] Optional encryption using Android Keystore
- [ ] Biometric unlock inside the app
- [ ] Export / import of data
- [ ] Search and categories

## 🤝 Feedback

I'm not a full-stack developer, and this is a learning project. Suggestions, issues, and pull requests are welcome. Open an [issue](https://github.com/sonaaar9n/PasswordKeyHEXA/issues) and tell me what you think.

## 📄 License

Released under the [MIT License](LICENSE). Copyright (c) 2026 Shivam Soni. 
