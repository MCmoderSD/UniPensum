# Store listing

The texts for the Google Play listing, in English. Play Console: Grow users → Store presence → Main store listing.
The texts are plain, without Markdown or HTML, because the Play Console shows them as typed. Copy the content of the
blocks as it is. One paragraph is one line, so do not wrap the lines, a line break would show in the store.

## App name
Limit 30 characters, this one has 9.
```
UniPensum
```

## Short description
Limit 80 characters, this one has 70.
```
A simple offline calendar for your university lectures, with reminders
```

## Full description
Limit 4000 characters, this one has 1683.
```
UniPensum is a simple calendar for Android with a focus on universities and lectures. It keeps your whole semester in one clear week view.

Create a semester, add your courses and enter lectures, exercises, labs and tutorials once. An event can repeat every week or every few weeks, so an exercise that takes place every second week is no problem. The week view shows the current time, lets you swipe from week to week and zoom in on the hours you need.

Every event can be held in person, online or hybrid. Add a room, a meeting link, a link to the course page and the lecturers with their e-mail address and phone number, and open them with one tap.

Plans change, so changes are easy. Edit a single session, this and all following sessions, or the whole series.

Get a reminder before every event, like in a calendar app. You choose how long before the start it arrives, in the settings or for a single event.

Back up all your data to a file and restore it later or on another device. The backup can be protected with a password.

Choose the light theme, the dark theme or follow your phone. On a tablet the week and the details are shown side by side. UniPensum is available in English and German.

No account, no ads and no internet access: all your data stays on your device. The app only asks for what the reminders need, which is notifications, alarms, and a restart of the alarms after the phone has been switched off.

UniPensum is open source under the BSD 3-Clause License. You can find the source code on GitHub, and you can report a bug, ask for a feature or add a translation there:
https://github.com/MCmoderSD/UniPensum
https://github.com/MCmoderSD/UniPensum/issues
```

## What the texts follow
From the [metadata policy](https://play.google.com/about/storelisting-promotional/metadata) and the
[help page for the listing](https://support.google.com/googleplay/android-developer/answer/9866151), checked on
2026-10-12. Check them again before the listing is published.

- Describe what the app really does, nothing more. No superlatives ("best", "#1"), no ranking, award or Google Play
  program words ("Editor's Choice", "New").
- No price or promotion words. That is why "free" is not in the texts, although the app is free.
- No emojis, no repeated special characters, no ALL CAPS (except acronyms), no keyword stuffing, no word lists.
- No testimonials, no comparison with other apps, no names or logos of third parties. That is why the texts say "a link
  to the course page", and not the name of the learning platform.
- Short description: no line break, no call to action, no time-sensitive wording. Periods only if there are several
  sentences.
- Keep the text short. A long or repetitive description can be a violation too.
- The text and the app must say the same: no internet access and no ads are true as long as the manifest has no
  `INTERNET` permission and the app has no ad library. Change the text when that changes.
- The permissions `POST_NOTIFICATIONS`, `USE_EXACT_ALARM`, `SCHEDULE_EXACT_ALARM` and `RECEIVE_BOOT_COMPLETED` are only
  for the reminders. The text calls the app a calendar with reminders, which is what the declaration for exact alarms
  in the Play Console has to say too.

## Links
- [Metadata policy](https://play.google.com/about/storelisting-promotional/metadata): rules for title, icon and
  descriptions
- [Add preview assets to showcase your app](https://support.google.com/googleplay/android-developer/answer/9866151):
  limits and formats of the texts, the icon, the feature graphic and the screenshots
- [Developer Program Policies](https://play.google.com/about/developer-content-policy/): read before the app is
  submitted
- [Advance notice to the app review team](https://support.google.com/googleplay/android-developer/answer/6320428): see
  below
- [Screenshots](../screenshots/PLAN.md) and [app icon](../design/README.md) of this repository, with their alt texts
- [Privacy policy](https://unipensum.mcmodersd.de/privacy/) of the app
- [Repository](https://github.com/MCmoderSD/UniPensum), [issues](https://github.com/MCmoderSD/UniPensum/issues)

## Advance notice
Google asks to be told before the listing is published only in a few cases: proof of rights to third-party brands or
content, permission of a government or health organization, a Korean gambling rating, an app with an accessibility
service, a validated non-profit, or documents of law enforcement or a regulator. None applies to UniPensum, so no
advance notice is needed. If one of them applies later, use the form on the linked page.

## Privacy policy
The Play Console needs a link to a privacy policy even for an app that collects nothing, and the app has to link to it
too. The URL for App content → Privacy policy:
```
https://unipensum.mcmodersd.de/privacy/
```
The page is in `pages/privacy/` (plain HTML and `pages/style.css`, no site generator) and is published with GitHub Pages
by the workflow `.github/workflows/pages.yaml`. It is served from the custom domain `unipensum.mcmodersd.de`: a CNAME
record at the domain provider points to `mcmodersd.github.io`, and the domain is set in the repository settings under
Pages. The record is proxied by Cloudflare, which the policy names in section 06; Cloudflare rewrites e-mail addresses
into a script, so the two addresses in the policy sit between `<!--email_off-->` comments to stay readable. The app links to it in Settings → About → Privacy policy (`privacy_url` in `strings.xml`). The
date at the top of the page ("Last updated") is its version history.

The policy follows what Google asks for: it is public, over HTTPS, a normal web page and not a PDF, it names the
developer and a contact (business@mcmodersd.de), the data the app handles, who sees it, how long it is kept and how to
delete it. `PrivacyPolicyTest` fails if a permission of the manifest is missing in the policy, if the app gets the
`INTERNET` permission, or if the link in the app and the address of the page differ.

Answers for the other forms in App content (as I understand the questions, read each one again in the Console):
- **Data safety:** the app does not collect or share any of the listed data types. There is no network access, no
  account, no SDK. The backup of Android is done by the system, not by the app, which the policy says openly.
- **Ads:** the app has no ads.
- **Permissions for exact alarms:** a calendar app whose reminders have to arrive on time (`USE_EXACT_ALARM`).
- **Target audience:** students, not children.

## Keeping it up to date
The features in the texts are the ones of the [README](../README.md). When a feature is added or removed, change both,
and count the characters again: the numbers above are the length of the text in the block, line breaks included.
A new permission or data flow also goes into `pages/privacy/index.html`, with a new date.
