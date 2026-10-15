# Brag Plan: UniPensum

## What is this app?
A simple calendar for Android with a focus on universities and lectures. Free and open source, without an account and
without internet access (README). The week view, the courses and the reminders are the product.

## The angle
A quiet premium product film in the black and white of the app. No jokes: UniPensum is earnest, and its strongest
claims are the things it leaves out. The five weekday bars of the app icon rise, the cap lands on them, and the bars
become the five days of the real week view. The second half is about what a real user does (open a lecture, set the
type of the event, get a reminder) and ends on the claims the README makes: no account, no internet access, open
source. The video is made from the real store screenshots, so it shows exactly what the Play listing shows.

It doubles as source material for the Play Store listing: the promo video (YouTube link in the Play Console) and the
ideas for the feature graphic (1024 x 500).

## Hook (first 2-3 seconds)
Black screen. Five white bars rise one after another, each to a different height, like the bars of the app icon. The
cap lowers onto them with a soft landing. Then "UniPensum" settles in under it. No text before the image.

## Key moments (the middle)
- The week view of the tablet screenshot (Winter 26/27, Mon to Fri) fills the frame; a slow push-in goes to the white
  line of the current time across the Tuesday "Algorithms" lecture, 8:00 to 11:15.
- Three phone screens slide in one by one: the details of a lecture (room, lecturer, "15 min before" reminder), the
  event form (In person / Online / Hybrid), the backup export (password optional).
- "No account." "No internet access." "Open source." one line at a time on black, then the name once more.

## Outro / punchline
The three lines are the punchline: the app that asks for nothing. Then the icon, "UniPensum" and the sentence from the
README, "A simple calendar for universities and lectures.", and the repository name in small type. Silence in the
picture, the music fades under.

## User flow worth showing
1. Entry: the week view with the current time marked (screenshots/tablet-7/tablet-7-01-week.png).
2. Key action: open a lecture and look at its details, then the form that sets in person, online or hybrid
   (screenshots/phone/phone-02-details.png, phone-05-event-form.png).
3. Result: the reminder in the details ("15 min before") and the backup that keeps all of it (phone-08-backup.png).

## Tone
- Preset: polished
- Creative direction: quiet premium product film in the black and white of the app
- Interpretation: few scenes, long holds, soft crossfades and slow pushes, nothing aggressive; the pace comes from
  clean motion, not from cutting. The product speaks for itself, text is short and always grounded in the README.

## Format: landscape — 1920x1080
## Duration: 22 seconds

## Visual identity (from the project)
- Background: #000000 (the app's dark theme `bg`), surface #1E1E21, divider #26262A
- Accent: #F5F5F6 (the app's accent in the dark theme is white); the only color comes from the course colors of the
  real screenshots (red #E5383B, orange #F77F00, green #2D9D5B, teal #14909B, blue #2563EB, violet #7C3AED)
- Text: #F5F5F6, secondary text #8F8F98
- Display font: Roboto Medium (the app uses `sans-serif-medium`; fall back to a clean system sans)
- Body font: Roboto Regular (`sans-serif`)
- Strongest visual element: the icon (white cap over five bars of random heights, 16/26/20/32/22 in a 130 box) and
  the real week view

## Source material
- Real UI: the store screenshots in `screenshots/` (all dark, sample data "Winter 26/27", the people and addresses in
  them are made up: `uni.example`, a +49 30 5550123 number). Nothing else leaves the project.
- Icon: `design/icon-dark.svg` (ids: background, mortarboard-top, mortarboard-body, bar-mon .. bar-fri)
- Copy that must appear verbatim: "A simple calendar for universities and lectures." (README, reworded from "with a
  focus on universities and lectures"), "No account." / "No internet access." (README: "without an account and
  without internet access"), "Open source." (README: "free and open source"; "free" is left out on purpose, it is a
  price word in the Play metadata policy), the labels of the app ("In person", "Online", "Hybrid", "Reminder").

## Share copy (draft)
UniPensum: a simple calendar for universities and lectures. No account, no internet access, open source.

## Audio direction
(Changed after the first renders: no sound at all, the video is silent. See "As built" at the end.)
- Role: sparse professional accents over a warm, steady bed
- Music: `happy-beats-business-moves-vol-12-by-ende-dot-app.mp3` (steady and clean, best for polished), 110 BPM
- Music treatment: starts at 0.0 at 0.3 volume, fade in over 0.5s, fade out over the last 1.5s; no ducking needed
- Music cue guidance: read `assets/music/cues/happy-beats-business-moves-vol-12-by-ende-dot-app.music-cues.md`. Beat
  grid around the scenes: 4.39, 4.91 (week reveal), 10.93, 12.02, 13.11 (the three phone cards, every other beat so
  the captions can be read), 16.38, 17.47, 18.56 (three claim lines), 19.66 (name). Strong cues to lock (at most
  three): 10.93 (first phone card), 13.11 (third phone card), 19.66 (final name).
- Audio-reactive treatment: subtle; the music's bass makes the soft glow behind the icon and the white line of the
  current time breathe a little. No waveform or equalizer shapes.
- SFX posture: sparse, motion-matched, quiet (0.55 to 0.7); a soft drop for each bar, a card sound for each phone, one
  soft bell at the final name
- Audio-coupled moments: the five bars rising, the three phone cards, the three claim lines, the final name
- Restraint rule: nothing loud, no riser, no whoosh; if a cue hurts readability, the text wins

## Storyboard

### Scene 1 — The icon builds — 4.5s
Black. Five white rounded bars (heights as in the icon, left to right) rise from one baseline in quick succession
(about 0.1s apart); the cap (diamond and body) lowers on top and settles. Then "UniPensum" fades in under it and, a
beat later, "A simple calendar for universities and lectures." Settled at least 2.2s before the exit.
Sequential/interaction: yes — the five bars appear one after another.
Audio intent: calm and confident, one soft landing for the cap
Audio-coupled idea: a quiet drop for each bar, the cap lands on the beat
Music: starts clean, steady
Transition mood: soft crossfade (0.6s), the bars dissolve into the week view → Scene 2

### Scene 2 — The week — 5.5s
The real week view (tablet screenshot, 16:9) fades in, large, with a slow push-in toward Tuesday morning. The white line
of the current time and the Algorithms lecture get a subtle glow. One short label below: "The week, at a glance." (a
framing line, not a claim), later "The current time is marked." (README).
Sequential/interaction: none (slow push-in)
Audio intent: calm, the first real product moment
Audio-coupled idea: the glow of the timeline breathes with the bass
Music: steady
Transition mood: soft slide up (0.5s) → Scene 3

### Scene 3 — Open a lecture — 6s
Three phone screens in a row, portrait, slide up one after another: lecture details, event form, backup. Each has one
short label that holds at least 0.8s settled: "Room, lecturer, reminder", "In person, online or hybrid", "Backup to a
file". Together on screen for the last second.
Sequential/interaction: yes — three cards arrive one by one, about 1.1s apart (every other beat), each stays.
Audio intent: tidy and satisfying, like cards being laid down
Audio-coupled idea: a card sound for each phone, aligned to the card's first visible frame
Music: steady
Transition mood: clean crossfade to black (0.5s) → Scene 4

### Scene 4 — Nothing asked — 6s
Black. One line at a time, large, centered: "No account." "No internet access." "Open source." Each line settles for
about 1s, the previous one leaves as the next arrives. Then the icon small above "UniPensum", the sentence "A simple
calendar for universities and lectures.", and "github.com/MCmoderSD/UniPensum" in small secondary type. The last 2.3s
are a still hold.
Sequential/interaction: yes — three lines, each held to the reading floor (short labels, 0.8s settled or more).
Audio intent: quiet payoff, then silence in the picture
Audio-coupled idea: a soft bell when "UniPensum" lands, the music fades out under it
Music: fade out over the last 1.5s
Transition mood: none, the video ends on the held frame

**Music mood for this video:** polished, steady
**Audio summary:** one clean bed, a handful of quiet motion-matched accents, one bell at the name, a fade at the end.

## Music cue guidance
- Track: vol-12, estimated 109.96 BPM, preset read from the skill.
- Strong cues used (at most three): 10.93s (first phone card), 13.11s (third phone card), 19.66s (final name).
- Beat grid for sequences: phone cards at 10.93, 12.02, 13.11; claim lines at 16.38, 17.47, 18.56. Text is revealed on
  every second beat or slower; the beats 0.55s apart are only for the bar accents of scene 1.
- Restraint: polished; the cues are optional, readability and the story come first.

## For the Play Store (after the video)
The feature graphic is made separately from the video; the finished file goes to `design/`.
Rules of the graphic (help page "Add preview assets"): 1024 x 500, JPEG or 24-bit PNG without alpha, key elements
away from the cut zones and near the center, no pure white, black or dark gray background (it vanishes into the Play
background), no device frames, store badges or third-party marks, no icon-like branding that repeats the app icon,
alt text of at most 140 characters.

## As built
The video follows the storyboard, with these differences:
- The outro does not repeat the sentence of scene 1: it shows the small icon, "UniPensum" and the repository name.
- The scenes overlap by about 0.5s for the soft crossfades (scene starts at 0, 4.39, 10.2 and 15.9 seconds).
- The three phones stay centered: the ones already on screen step aside as the next one arrives.
- The week view and the phones have square corners: rounded corners cut off the clock of the screenshots at the top left.
- **No sound at all.** The pops, thuds and the bell hurt more than they helped (the card sounds and the soft thuds of
  the phones were tried first), and without them the music was not needed either: the video is silent, the cleanest
  version. Every audio line of the storyboard, the audio direction and the music cue guidance above no longer applies;
  the music, the sound effects and the audio data of the glow are removed from the composition.
- The glow behind the icon and the line of the current time are now fixed (they used to follow the music). The timing of
  the scenes is still the one of the 110 BPM grid it was planned on: the cap at 1.09s, the phones at 10.93, 12.02 and
  13.11s, the three claims at 16.38, 17.47 and 18.56s, the name at 19.66s.
- `hyperframes check` passes with 0 errors; the 4 warnings only suggest sub-compositions for the Studio timeline.

## Files
Only this plan is in git (see `.gitignore`). The composition, the rendered video, the poster, the caption and the
tool files are made by the brag skill and stay on the machine that made them. The video is final and has no audio.
