PasswordKeyHEXA 🔐

Your Passwords. Your Phone. Your Control.

🌐 Language: "हिंदी" (README.md) | English

---

About PasswordKeyHEXA

PasswordKeyHEXA is a lightweight, free, and open-source offline password manager for Android. It is designed to help you organize passwords for your social media accounts, websites, and other online services in one place.

The focus is on straightforward password management. You do not need to remember another master password or go through a mandatory in-app login process every time you want to access your saved passwords.

No account registration, no app-managed cloud synchronization, no advertisements, and no mandatory master password.

Why Use PasswordKeyHEXA?

- Your data stays on your device: The app is not designed to send your passwords to its own online servers.
- Direct password access: Open the app and access your saved information without an additional master password.
- Multiple accounts, one place: Organize passwords for several accounts on the same social media platform or website.
- Password history: Keep a Current Password along with Previous Password 1 and Previous Password 2. Date and time information helps distinguish older entries from newer ones.
- Simple management: View, copy, and edit saved passwords.
- Device migration: Use Encrypted Backup to restore supported backup data on another Android device.

PasswordKeyHEXA is intended for users who prefer to manage their passwords locally without depending on an online account.

Features

- Store passwords for social media, websites, and other accounts.
- View and manage saved account details.
- View, copy, and edit passwords.
- Maintain a current password and two previous password entries.
- Organize password entries using date and time information.
- Fully offline operation.
- No Internet permission required.
- No account registration.
- No advertisements or trackers.
- Separate Normal Backup and Encrypted Backup options.
- Restore Encrypted Backups on another Android device.
- Export password information in Excel and PDF formats.

---

Security & Privacy

PasswordKeyHEXA follows a deliberately simple security model. It avoids requiring an additional master password or mandatory in-app authentication for everyday access.

1. Local Data Storage

- Account details are stored in the device's local storage.
- The app uses an SQLite database.
- The internal database is intentionally not encrypted.
- Android Application Sandbox isolates the app's private data from other ordinary Android applications.
- The app does not require Internet permission.
- No cloud account or online synchronization is required to use the app.

Why is the internal database not encrypted?

This is an intentional design decision. The aim is to avoid adding a separate database-encryption or authentication step to everyday password access.

Android Application Sandbox and database encryption are different security mechanisms. Under normal Android conditions, the Sandbox isolates an app's private data from other ordinary apps, but it does not encrypt the SQLite database itself.

If someone obtains the database through root access or another privileged method, its contents may be readable.

About Android device backups: The app itself does not transmit your passwords to a server over the Internet. However, Android System Backup is a separate operating-system feature. Depending on your device and backup settings, app data may be included in a system backup. This is different from the app directly sending data to a server.

2. Use Your Device's Screen Lock and App Lock

PasswordKeyHEXA does not enforce a separate master password or mandatory in-app login.

After installing the app, enable your Android device's available security features:

- Set a Screen Lock on your phone.
- Enable the manufacturer's built-in App Lock for PasswordKeyHEXA, if available.
- Use fingerprint or PIN-based App Lock protection where supported.
- Do not leave your unlocked phone accessible to others.

Important: If you do not use Screen Lock or App Lock on your device, this application may not be suitable for your security requirements. Device-level App Lock plays an important role in this app's security model.

3. Backup Security

PasswordKeyHEXA provides two separate backup options.

Normal Backup

- Creates an unencrypted backup file.
- Account information remains readable inside the file.
- Cannot be restored using the app's Import feature.
- Anyone who obtains the file may be able to read its contents.
- Users are responsible for storing the file securely.

Warning: Do not share Normal Backup files. Keep them in a secure location.

Encrypted Backup

- Creates a backup in ".pkbackup" format.
- Uses AES-256-GCM encryption.
- Protected by a user-defined 6-digit PIN.
- Requires the correct PIN for restoration.
- Can be imported and restored on another Android device.
- Does not depend on the original device's Android Keystore key.

PIN security: A 6-digit PIN has one million possible combinations. Encryption protects the backup contents, but a short PIN does not provide unlimited resistance against every offline attack. Choose a PIN that you do not reuse for another account or service.

If you forget the backup PIN, the encrypted backup cannot be recovered.

4. Excel and PDF Exports

Excel and PDF export files are not encrypted.

Anyone who obtains these files may be able to read the passwords and account information they contain.

Export only when necessary and store the files securely.

---

Backup & Restore

Encrypted Backup is useful when moving to a new Android phone or using the app on another device.

Normal Backup

Normal Backup creates an unencrypted file. It cannot be restored through the app's Import feature.

Restore Using Encrypted Backup

1. Install PasswordKeyHEXA on the new Android device.
2. Transfer your ".pkbackup" file to the new device.
3. Open PasswordKeyHEXA.
4. Select the Import option.
5. Choose your encrypted backup file.
6. Enter the correct 6-digit PIN.
7. Review the import preview.
8. Confirm the import.

Import Behavior

- Existing newer data is not automatically overwritten.
- A password or username from the backup is imported only when its associated date is newer than the corresponding existing entry.
- Older or equal-date entries are skipped.
- An import preview is displayed before confirmation.

This helps reduce the risk of replacing newer information with data from an older backup.

Recommendation: Create an Encrypted Backup before uninstalling the app or changing your device.

---

Download

1. Visit "GitHub Releases" (https://github.com/sonaaar9n/PasswordKeyHEXA/releases).
2. Download the available APK.
3. Install it on your Android device.
4. Enable the required installation permission if prompted.
5. Enable your device's Screen Lock and App Lock after installation.

Install APK files only from trusted sources.

---

Screenshots

Actual application screenshots can be added here.

---

Build From Source

Clone the repository:

git clone https://github.com/sonaaar9n/PasswordKeyHEXA.git

1. Open the project in Android Studio.
2. Allow Gradle Sync to complete.
3. Select Run or Build APK(s).

Technology Stack

- Kotlin
- Android SDK
- SQLite
- Gradle

Package Name: "com.mykey.passwords"

---

Future Improvements

- Search and categories.
- Improved account organization.
- User experience enhancements.

---

Feedback & Contributions

PasswordKeyHEXA is an independent open-source project.

Suggestions, bug reports, and contributions are welcome.

For issues or suggestions, please use the GitHub Issues section.

---

License

Released under the MIT License.

Copyright (c) 2026 Shivam Soni.
