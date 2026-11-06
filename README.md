# Diary

A monthly-calendar diary for Android. It works fully offline: entries are kept in a private file on the phone (`entries.json`, inside the app's own storage). It has no internet permission.

Export: Markdown (this month or whole diary), plain text, and a JSON backup that you can restore later.

## Build the APK

**Option A, Android Studio**
1. Install Android Studio (developer.android.com/studio).
2. *Open* this folder and wait for the Gradle sync to finish.
3. Menu *Build › Build App Bundle(s) / APK(s) › Build APK(s)*.
4. Click *locate* in the notification. The file is `app/build/outputs/apk/debug/app-debug.apk`.

**Option B, GitHub (no installation)**
1. Create a new private repository on github.com and upload the contents of this folder, including the `.github` folder.
2. Open the *Actions* tab and wait for "Build APK" to show a green tick (about 5 minutes).
3. Open the run and download *Diary-apk* (a zip that contains `app-release.apk`).

## Install on the phone

1. Copy the APK to the phone (USB cable, Google Drive, or email it to yourself).
2. Open it from the *Files* app, or tap the download.
3. When asked, allow *Install unknown apps* for that app (Files, Drive, Chrome…), go back, then tap *Install*.
4. If Play Protect warns about an unknown developer, tap *More details › Install anyway*.

## Updating

Every build is signed with the same key (`app/diary-release.jks`). A new version therefore installs over the old one and keeps your entries. Keep that file. Uninstalling the app deletes the entries, so export a backup first.
