# Store screenshots

The screenshots for the Google Play listing: three sets of eight, all in the dark theme, all with the same data and
the same time (Tuesday, 13 October 2026, 9:41). Everything needed to take them again is in this file and in `tools/`.

| Folder       | For            | Pixels    | Files                                                |
|--------------|----------------|-----------|------------------------------------------------------|
| `phone/`     | Phone          | 2160×3840 | `phone-01-week.png` to `phone-08-backup.png`         |
| `tablet-7/`  | 7-inch tablet  | 3840×2160 | `tablet-7-01-week.png` to `tablet-7-08-backup.png`   |
| `tablet-10/` | 10-inch tablet | 7680×4320 | `tablet-10-01-week.png` to `tablet-10-08-backup.png` |

The file name starts with the device set, so an image is still clear after it has been moved or uploaded.

`sample-data.unipensum` is the data of all 24 images, a backup of the app. `tools/` has the scripts that were used.

## What Google Play asks for
From [Add preview assets to showcase your app](https://support.google.com/googleplay/android-developer/answer/9866151),
read on 2026-10-13. Check it again before an update.

- Up to 8 screenshots for each device type (phone, 7-inch tablet, 10-inch tablet, …).
- JPEG or **24-bit PNG without alpha**. Phone: 320 to 3840 px, the long side at most twice the short one.
- Tablets and Chromebooks: at least 4 screenshots, 1080 to 7680 px, **16:9** for landscape and 9:16 for portrait.
- Recommended: at least 4 screenshots of at least 1080 px, showing the real app, no device frames, no additional text
  or backgrounds, no people or fingers. Status bar: battery and Wi-Fi full, no notifications, no provider.
- An alt text of at most 140 characters for each image (below).

## Devices
Three emulators with the system image `android-37.2 google_apis_ps16k x86_64` (no Play Store, so no dialogs). The
densities in their configuration are 640 dpi. The script overrides them with `wm density`, so that all three have the
same logical height of about 786 dp and the editors fit on a page in the same way:

| Set       | Emulator                  | Pixels    | Density | dp       | Layout                                                                    |
|-----------|---------------------------|-----------|---------|----------|---------------------------------------------------------------------------|
| phone     | `Screenshot_Phone`        | 2160×3840 | 780 dpi | 443×788  | phone, about as wide as a Pixel 10 Pro XL (448 dp; a Pixel 10 has 411 dp) |
| tablet-7  | `Screenshot_Tablet_Small` | 3840×2160 | 440 dpi | 1396×785 | tablet (from 600 dp) with calendar and pane side by side (from 720 dp)    |
| tablet-10 | `Screenshot_Tablet_Large` | 7680×4320 | 880 dpi | 1396×785 | the same layout as tablet-7, with four times the pixels                   |

Start them one at a time: `emulator -avd <name> -port 5556 -no-window -no-audio -no-boot-anim -no-snapshot -gpu host`.
The 8K emulator does not start with `-gpu host` (the log says `Failed to find ColorBuffer`): use
`-gpu swiftshader_indirect`. Its first boot takes about a quarter of an hour.

## The data
`sample-data.unipensum` holds the semester "Winter 26/27" (5 October 2026 to 22 January 2027) with 6 courses,
11 series (168 sessions) and 6 lecturers. Lectures last 90 minutes or 3 h 15 min, the lunch break from 13:00 to
14:00 is free, nothing overlaps. Slots: 8:00, 9:45, 11:30, 14:00, 15:45 (a double slot makes 3 h 15 min).

| Day | Morning                                                                                         | Afternoon                                                     |
|-----|-------------------------------------------------------------------------------------------------|---------------------------------------------------------------|
| Mon | 9:45–11:15 Analysis I, lecture, A1, Weber                                                       | 14:00–15:30 Software Engineering, lecture, hybrid, H3, Sommer |
| Tue | **8:00–11:15 Algorithms, lecture, H1, Neumann (reminder 15 min)**                               | 14:00–15:30 Databases, lecture, H2, Brandt                    |
| Wed | 9:45–11:15 Linear Algebra, exercise, C 0.12, Koch, every 2nd week                               | 14:00–17:15 Algorithms, lab, online, Brandt                   |
| Thu | 8:00–11:15 Physics, lecture, hybrid, A2, Lang                                                   | 14:00–15:30 Analysis I, exercise, B3, Koch                    |
| Fri | 8:00–9:30 Databases, lab, Lab 3, Brandt · 11:30–13:00 Software Engineering, tutorial, Library 2 | 14:00–15:30 Linear Algebra, lecture, H1, Weber                |

At 9:41 on Tuesday the Algorithms lecture is running, so the line of the current time crosses it. The other reminders
are the defaults (30 minutes, 5 for an online event). Lecturers: Anna Weber (mail, phone), Michael Koch (mail),
Sabine Lang (phone), Jonas Neumann (mail, phone), Tobias Brandt (mail, phone), Lena Sommer (mail).

The file comes from the debug seeder `DebugSeeder` (debug builds only):
`adb shell am start -n de.mcmodersd.unipensum/.ui.MainActivity --ez unipensum.debug.seed true`, then Settings →
Export backup without a password. **Change the data in the seeder, seed, export again and replace the file**, never
edit it by hand. Every emulator imports this file (Settings → Import backup → Replace all data), so the data are the
same in all images.

## Time and status bar
- The clock is set to Tuesday 13 October 2026, 09:41:00 in Europe/Berlin (CEST): `settings put global auto_time 0`,
  `settings put global auto_time_zone 0`, `cmd alarm set-timezone Europe/Berlin`,
  `cmd alarm set-time 1791877260000`. The line of the current time in the app follows the system clock, so the script
  sets it again just before every screenshot.
- Dark theme: `cmd uimode night yes`, and the app's own setting (Settings → Appearance) is on **Dark**.
- Status bar: the demo mode of the system UI (`am broadcast -a com.android.systemui.demo`) shows the clock 9:41, a full
  battery, full Wi-Fi and no notification icons. The mobile signal is left out because the demo mode never draws it
  with all bars. `tools/lib.sh` has the commands (`demo`, `cleanbar`).
- Also needed: `svc data disable` (without it the status bar says "3G"), `cmd location set-location-enabled false` (a
  satellite icon), and the two notifications of the emulator ("Set a screen lock", "Serial console enabled") cleared
  from the notification shade (a shield icon).

## The eight images
The file of an image is `<set>-<name>.png`, for example `tablet-7-01-week.png`. The order tells the story: the week,
one lecture, the courses behind it, then how to change things, and at the end the settings and the backup. On a tablet
the pages from 3 on open in the pane at the right and the week stays visible.

| # | Name               | Shows                                                                 | Path in the app                                |
|---|--------------------|-----------------------------------------------------------------------|------------------------------------------------|
| 1 | `01-week`          | The week, Monday to Friday, the line of the current time at 9:41      | start screen                                   |
| 2 | `02-details`       | A lecture: time, room, lecturer with mail and phone, reminder, Moodle | tap "Algorithms, Lecture"                      |
| 3 | `03-courses`       | The six courses with color and number of events                       | button "Courses"                               |
| 4 | `04-course-editor` | Name, Moodle link, colors, events of a course                         | course "Physics" (one event, so the page fits) |
| 5 | `05-event-form`    | Type, weekday, start and end, format, room                            | event of the course editor                     |
| 6 | `06-lecturers`     | The lecturers                                                         | Settings → Manage lecturers                    |
| 7 | `07-settings`      | Theme, visible hours, reminders                                       | button "Settings"                              |
| 8 | `08-backup`        | Export of a backup, with the password switched on                     | Settings → Export backup                       |

The tap targets are found by their English texts, so the English app is needed. For German images the labels in
`tools/capture.sh` have to be changed.

### Alt texts (for the Play Console, at most 140 characters)
| # | Alt text                                                                                                                        |
|---|---------------------------------------------------------------------------------------------------------------------------------|
| 1 | Week view from Monday to Friday with colored lectures and a line marking the current time on Tuesday morning.                   |
| 2 | Details of a lecture with time, room, lecturer with e-mail and phone number, reminder, and buttons for Moodle, edit and delete. |
| 3 | The six courses of the semester, each with its color and number of events, and a button for a new course.                       |
| 4 | Course editor with name, Moodle link, a choice of colors and the events of the course.                                          |
| 5 | Event form with type, weekday, start and end time, in-person, online or hybrid format, and room.                                |
| 6 | List of the lecturers with a button for a new lecturer.                                                                         |
| 7 | Settings with light, dark or system theme, visible hours, reminders and how lecturers are named.                                |
| 8 | Export of all data as a backup file, protected with a password if wanted.                                                       |

On the tablets add "Next to the week view." to 3 to 8.

## Taking them again
1. `./gradlew assembleDebug`. A debug build is only needed to seed new data, the import of the sample file works
   in every build.
2. Start the emulator of the set (see Devices) and wait for `sys.boot_completed`.
3. In `tools/` (Git Bash, `ADB` points to `adb.exe` if it is not on the PATH):
   ```bash
   SET=phone SER=emulator-5556 ./capture.sh prep 780      # density, dark mode, clock, install, clean status bar
   SET=phone SER=emulator-5556 ./capture.sh import        # the sample data and the Dark theme
   SET=phone SER=emulator-5556 ./capture.sh shoot         # the eight raw screenshots into tools/work/phone
   ```
   The densities are 780 (phone), 440 (tablet-7) and 880 (tablet-10).
4. `powershell -ExecutionPolicy Bypass -File postprocess.ps1` converts them to 24-bit PNG, removes the window handle
   on the tablets, names them `<set>-<name>.png` and checks size, format and file size. It writes into this folder.
5. Look at every image. Then upload them in the Play Console: Grow users → Store presence → Main store listing →
   Graphics, one list per device type, with the alt texts.

## Pitfalls
- **Window handle:** on the tablets the system draws a small bar at the top center (the handle of the desktop window
  mode). It cannot be switched off from the shell. `postprocess.ps1` paints it over with the black of the status bar
  (4K: x 1809 to 2029, y 27 to 37; 8K: x 3620 to 4059, y 55 to 76). The first screenshot after a boot may also have a
  tooltip "Tap handle to access menu and display options": restart the app and wait before the first image.
- **uiautomator** sometimes returns "null root node" right after the app was restarted, especially at 8K: wait and try
  again. `tap` takes the last match because the screens below stay in the dump.
- The settings of the emulator stay on the emulator (density, night mode, clock). `wm density reset` undoes the
  density. The overrides do no harm to anything else, the emulators are only for the screenshots.
- Running scripts is blocked in PowerShell by default, hence `-ExecutionPolicy Bypass` for the one call.