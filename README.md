# UniPensum

## Description
A free and open source timetable app for Android. \
UniPensum is a replacement for the timetable of QIS/LSF (HIS eG) and works for any university: a simple week calendar for the lectures of a semester, without an account and without extra features in the way.

The app works completely offline. It needs no system permissions and has no internet access.

## Features
- [x] Week grid from Monday to Friday, swipe between weeks
- [x] Configurable visible hours (7:00 to 22:00 by default)
- [x] A line marks the current time in the week grid, and the current hour is highlighted
- [x] Several semesters with automatic names such as `WiSe 26/27` or `SoSe 27`
- [x] A new semester runs 16 weeks (about four months) and a new event lasts 3 h 15 min by default
- [x] Courses with one or more recurring events (lecture, exercise, lab, tutorial)
- [x] Weekly, every 2 weeks, or every n weeks
- [x] In person, online and hybrid events with room and meeting link
- [x] Moodle link per course
- [x] Lecturers with name, e-mail and phone number, chosen per event; a tap opens the mail or phone app
- [x] Lecturers shown by last name or by first and last name
- [x] Edit or delete a single session, this and all following sessions, or the whole series
- [x] Move a single session to another day
- [x] Overlapping sessions are marked
- [x] Light and dark theme, following the system or set manually
- [x] English and German
- [x] Export and import of all data as a `.unipensum` file, optionally protected with a password
- [ ] Reminders
- [x] Tablet layout: the calendar stays in view next to the settings, the courses and the editors
- [ ] Landscape on phones

## Requirements
- Android 12 (API 31) or newer

## Installation
There is no release yet. Releases are planned for GitHub Releases and Google Play. \
Until then, build the app from source.

## Build

### Android Studio
Open the project in Android Studio and run the `app` configuration.

### Command Line
Use the JDK that ships with Android Studio as `JAVA_HOME`, then build the debug APK:
```bash
./gradlew assembleDebug
```
The APK is written to `app/build/outputs/apk/debug/app-debug.apk`.

### Sample Data
Debug builds can replace all data with a sample semester. This deletes the existing timetable.
```bash
adb shell am start -n de.mcmodersd.unipensum/.ui.MainActivity --ez unipensum.debug.seed true
```

## Tests
Unit tests for the timetable logic run on the development machine:
```bash
./gradlew testDebugUnitTest
```
Database tests (including the migration from older database versions) run on a device or emulator:
```bash
./gradlew connectedDebugAndroidTest
```
> [!WARNING]
> `connectedDebugAndroidTest` runs on every connected device and uninstalls the app afterwards, which deletes its data.
> Disconnect your own phone and use an emulator, or install the two APKs on the emulator yourself:
> ```bash
> ./gradlew assembleDebug assembleDebugAndroidTest
> adb -s emulator-5554 install -r app/build/outputs/apk/debug/app-debug.apk
> adb -s emulator-5554 install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
> adb -s emulator-5554 shell am instrument -w de.mcmodersd.unipensum.test/androidx.test.runner.AndroidJUnitRunner
> ```

## Release
Releases are built by a GitHub Actions workflow, [`android-build-release.yaml`](.github/workflows/android-build-release.yaml).
It runs on every push to `master` or `main` whose commit message contains `release`, or manually from the Actions tab.

### Version
The version is set in one place, `appVersion` at the top of `app/build.gradle.kts`. The version code Android needs is
derived from it: `1.2.3` becomes `10203` (one to three parts, each from 0 to 99). A version ending in `-SNAPSHOT` is a
development build.

### Publish a release
1. Set `appVersion` to the release, for example `"1.1"`.
2. Commit with `release` in the message and push.
3. The workflow runs the unit tests and lint, builds the signed APK and App Bundle (AAB) and creates the GitHub release
   `1.1` with both files, the checksums of the APK and the fingerprint of the signing certificate.
4. Set `appVersion` to the next development version, for example `"1.2-SNAPSHOT"`.

| Version                     | Result                                                                                         |
|-----------------------------|------------------------------------------------------------------------------------------------|
| Ends with `-SNAPSHOT`       | Tests and build run, the signed files are attached to the workflow run for 14 days, no release |
| No suffix, not released yet | A release is created                                                                           |
| No suffix, already released | The run fails: raise `appVersion`                                                              |

### Signing key
Every release is signed with one key, and Android only installs an update that is signed with the same key as the
installed app. **Keep the keystore and its password safe** (password manager plus an offline copy): if it is lost, no
update can reach the people who installed the app, and GitHub never shows a secret again.

The key must be RSA. Android also accepts ECDSA but not Ed25519, and Google Play only accepts RSA with at least 2048 bits
and a validity beyond 22 October 2033. Create it once (`keytool` is in the `jbr/bin` folder of Android Studio):
```bash
keytool -genkeypair -v -keystore unipensum-release.jks -storetype PKCS12 -alias unipensum -keyalg RSA -keysize 4096 -validity 10000
```
Then hand it to the workflow, either from Git Bash or from PowerShell. The passwords are asked for and never appear in the
command line (with PKCS12 the key password is the store password, so enter the same one twice):
```bash
base64 -w0 unipensum-release.jks | gh secret set SIGNING_KEYSTORE
gh secret set SIGNING_STORE_PASSWORD
gh secret set SIGNING_KEY_PASSWORD
gh variable set SIGNING_KEY_ALIAS --body unipensum
```
```powershell
[Convert]::ToBase64String([IO.File]::ReadAllBytes("unipensum-release.jks")) | gh secret set SIGNING_KEYSTORE
gh secret set SIGNING_STORE_PASSWORD
gh secret set SIGNING_KEY_PASSWORD
gh variable set SIGNING_KEY_ALIAS --body unipensum
```
For a signed build on your own machine, put the same data into `keystore.properties` in the project folder (it is
ignored by git). Without it, `assembleRelease` produces an unsigned APK:
```properties
storeFile=C:/path/to/unipensum-release.jks
storePassword=...
keyAlias=unipensum
keyPassword=...
```
Build provenance attestations are added to the release files as soon as the repository is public.

> [!NOTE]
> An app signed with another key cannot be updated in place. Installing a release over a debug build needs the debug
> build removed first, which deletes its data: export a backup (Settings → Data) beforehand and import it afterwards.

## Backup
Settings → Data saves everything (semesters, courses, sessions, lecturers; not the settings) to a file and
restores it. The file and where it goes are chosen in the system's own dialogs, so the app needs no storage
permission. An import replaces all data after an explicit confirmation, all or nothing; a backup from another
version of the app imports with a warning.

A `.unipensum` file is a zip archive, so any zip tool can look inside:

| Entry           | Content                                                                                                           |
|-----------------|-------------------------------------------------------------------------------------------------------------------|
| `manifest.json` | Always readable: file format, database version, app version, date and, for a protected backup, the key parameters |
| `data.json`     | The data as JSON: lecturers, semesters, courses, series and sessions (without a password)                         |
| `data.enc`      | In place of `data.json` for a protected backup: the same JSON, deflated, then encrypted                           |

The password protects the data with AES-256-GCM; the key is derived with PBKDF2-HMAC-SHA-256 (600 000 iterations,
random salt). The manifest is authenticated too, so a changed file is refused like a wrong password. A forgotten
password cannot be recovered.

The data is JSON on purpose and not an SQL dump: importing never runs anything from the file, and every field is
checked and cleaned like typed text. An older or newer file is read as far as it is understood: unknown fields
are ignored, unknown values get a default, and entries that break a rule of the app are skipped and counted.

## Project Structure
The app is written in Java with the classic Android View system. All data is stored in a local SQLite database.

```
de.mcmodersd.unipensum
├── domain              Plain Java without Android imports, covered by unit tests
│   ├── model           Semester, Course, Series, Session and their value types
│   ├── logic           Recurrence, semester rules, week layout, series editing
│   ├── text            Cleaning and checking of typed text, web links, e-mail addresses and phone numbers
│   └── backup          What a backup holds, and the checks that make imported data safe
├── data                Repository, settings and the read models of the UI
│   ├── db              SQLite schema, migrations and data access
│   └── backup          The .unipensum file: zip container, JSON, encryption, export and import
└── ui
    ├── week            The week grid, the app's main screen
    ├── session         Session details, edit scope and the event form
    ├── course          Course editor and course list
    ├── lecturer        Lecturer list, picker and editor
    ├── semester        Semester list and editor
    ├── settings        Theme, language, visible hours and how lecturers are named
    ├── backup          Export and import sheets
    ├── format          Formatting of dates, times and names
    └── widget          The app's own controls, sheets and pickers
```

| Term in the app | Class      | Meaning                                                    |
|-----------------|------------|------------------------------------------------------------|
| Semester        | `Semester` | A period with a start and an end                           |
| Course          | `Course`   | A subject with a name, a color and an optional Moodle link |
| Lecturer        | `Lecturer` | A person who teaches, chosen for an event                  |
| Event           | `Series`   | A recurring event of a course                              |
| Session         | `Session`  | A single occurrence on a concrete date                     |

## License
UniPensum is released under the [BSD 3-Clause License](LICENSE).