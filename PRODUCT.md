# Product

<!-- impeccable:product-schema 1 -->

## Platform

android

The app is native Android (Java 17, classic View system, minSdk 31). The website under `pages/` (start page, privacy
policy, imprint, 404) is a small companion on the web, published with GitHub Pages at `unipensum.mcmodersd.de`.

## Users

Students at universities and other higher education institutions, in any country. They keep the timetable of their
semester in the app: lectures, exercises, labs and tutorials. Most of the time they are on the move and want to see at a
glance what comes next, where it takes place and whether it is online. At the start of a semester they enter their
courses once; during the semester they adjust single sessions when plans change.

The app is not made for children; the Play target audience is students.

## Product Purpose

UniPensum is a simple calendar for Android with a focus on universities and lectures. It keeps the whole semester in
one week view, reminds before every event and keeps all data on the device. Success means a student sets up a semester
once and from then on trusts the app to show the right session, room and link at the right time.

## Positioning

A calendar built for university life that sends nothing to the internet. The combination is the point: semesters,
courses, events that repeat every n weeks, in-person, online and hybrid sessions with room, meeting link, course page
link and lecturers, and at the same time no account, no internet access, no ads and open source code.

## Operating Context

- **Daily use:** a quick look on the phone between lectures or on the way: the current week, the current time marked,
  the next session with its room or link, one tap to open a meeting link, the course page, or a lecturer's e-mail
  address or phone number.
- **Semester setup:** create a semester, add courses with a name and a color, enter each event once with its rhythm.
- **Changes during the semester:** edit a single session, this and all following sessions, or the whole series.
- **Reminders:** a notification before every event, with a lead time set in the settings or per event.
- **Backup:** export all data to a `.unipensum` file, optionally protected with a password, and import it later or on
  another device.
- **Devices:** phones first; on a tablet the week and the details sit side by side.

## Capabilities and Constraints

- **Features:** week calendar (swipe between weeks, pinch to zoom, current time marked); semesters and courses;
  recurring events (lecture, exercise, lab, tutorial), weekly or every n weeks; in person, online and hybrid with room,
  meeting link, course page link and lecturers; edit scopes (one session, following sessions, whole series);
  reminders; backup and restore with optional password; light, dark and system theme; tablet layout.
- **Terminology:** Semester (a period with a start and an end), Course (a subject with a name, a color and an optional
  course page link), Lecturer (a person who teaches, chosen for an event), Event (a recurring event of a course, class
  `Series`), Session (a single occurrence on a concrete date). German semester names follow the local convention
  (WiSe/SoSe).
- **No network:** the manifest has no `INTERNET` permission, and `PrivacyPolicyTest` fails if it gets one. The only
  permissions are the ones the reminders need: `POST_NOTIFICATIONS`, `USE_EXACT_ALARM`, `SCHEDULE_EXACT_ALARM`,
  `RECEIVE_BOOT_COMPLETED`.
- **Data:** a local SQLite database; no account, no SDKs, no analytics, no ads. Android's own backup is done by the
  system, which the privacy policy says openly.
- **Languages:** English and German; English is the fallback, so a translation can arrive in parts. New languages come
  from contributors as one `values-xx/strings.xml`. The language is picked in the Android settings. A language that
  is written from right to left mirrors the whole app, the week included.
- **Distribution:** GitHub Releases and Google Play, both not yet published ("coming soon" in the README).
- **Price:** free. Store texts must not say "free" (Play metadata policy), even though it is true.
- **License:** BSD 3-Clause, Seraphin Berger (MCmoderSD).

## Brand Commitments

- **Name:** UniPensum, written as one word with two capitals.
- **Developer:** Seraphin Berger (MCmoderSD); public contact `business@mcmodersd.de`.
- **Icon:** a mortarboard over five bars, one for each weekday of the timetable, in black and white with a light and a
  dark variant (`design/`). The feature graphic shows the name, the one-line description and the five bars, without the
  cap.
- **Voice:** plain and factual. Texts describe what the app really does and nothing more: no superlatives, no ranking or
  award words, no emojis, no ALL CAPS, no keyword lists, no comparisons with other apps, no third-party names or logos
  in store texts ("course page", not the name of a learning platform). The README and the store texts say the same.
- **One-line description:** "A simple calendar for universities and lectures" (feature graphic) and "A simple offline
  calendar for your university lectures, with reminders" (Play short description).

## Evidence on Hand

- `README.md`: features, project structure, terminology, translation guide.
- `docs/store-listing.md`: the Play texts and the policy rules they follow, Data safety and other Play Console answers.
- `screenshots/`: 24 Play Store screenshots with sample data, made on emulators (see `screenshots/PLAN.md`).
- `design/`: app icon SVGs and 512 px PNGs, feature graphic SVG and PNG, with alt texts.
- `pages/`: start page, privacy policy (`pages/privacy/`), 404 page.
- `brag-output/`: a short launch video and share copy.
- **Absent, never to be invented:** users, download numbers, ratings, reviews, testimonials, press, awards, partner
  universities. The app is not published yet.

## Product Principles

1. **Nothing leaves the device.** No account, no network, no ads, no tracking. A feature that would need the internet is
   a product decision with a privacy policy change, never a convenience.
2. **University life is the model.** Semesters, courses, every-n-weeks rhythms and in-person, online and hybrid
   sessions are first-class; the app speaks the student's terms and does not force them into a generic calendar.
3. **The next session in one look.** Daily use is a glance on the move: what is next, where, and is it online. Setup can
   take a few minutes once per semester; daily use cannot.
4. **Say what is true, plainly.** Texts in the app, the store and the README describe exactly what the app does.
5. **Usable by everyone.** Accessibility is part of done, not a later pass.

## Accessibility & Inclusion

- Every screen can be used with TalkBack, including the app's own controls (the week grid, the month calendar, the
  color picker, switches and pickers); custom views expose their parts as accessible nodes.
- Text follows the system font size and layouts hold when it grows.
- Contrast meets at least WCAG 2.2 AA, in the light and in the dark theme.
- Two languages today; every visible text lives in `strings.xml` so translations can be added without code changes.
