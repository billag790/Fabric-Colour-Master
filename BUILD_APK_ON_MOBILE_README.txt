FABRIC COLOUR MASTER — BUILD APK USING ONLY YOUR PHONE
======================================================

Important: This ZIP is source code, not an APK. This workflow asks GitHub's online
build computer to compile the APK, so Android Studio is not required on your phone.
You need a GitHub account and an internet connection for the build/download steps.

STEPS
-----
1. Download and extract this ZIP on your phone.
2. Open https://github.com in Chrome and sign in/create an account.
3. Create a NEW PUBLIC or PRIVATE repository, e.g. fabric-colour-master.
4. Upload the CONTENTS of this project folder into the repository (not the ZIP itself).
   Make sure settings.gradle, build.gradle, gradle.properties, app/ and .github/
   are at the repository's top level.
5. Open the repository's Actions tab. If asked, enable Actions.
6. Choose "Build Fabric Colour Master APK" and tap "Run workflow".
   If the workflow does not appear, check that .github/workflows/build-apk.yml was uploaded.
7. Wait for the workflow to finish with a green check mark.
8. Open the completed run, scroll to Artifacts, and download
   "Fabric-Colour-Master-debug-apk".
9. Extract the downloaded artifact ZIP. It contains app-debug.apk.
10. Tap app-debug.apk to install. Android may ask you to allow installs from Chrome/Files;
    only allow this for the app you used to open the APK, and turn the permission off after.

APP NOTES
---------
- Saved records are stored locally in the app's SQLite database.
- Uninstalling the app or clearing its data can delete saved records. Use Export Backup.
- Colour matching is approximate; lighting and fabric texture affect results.
- This project has not been built or device-tested in this chat. If the online build fails,
  share a screenshot or the error log and I can help fix it.
