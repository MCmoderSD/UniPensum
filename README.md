# UniPensum

A simple calendar for Android with a focus on universities and lectures. 
Free and open source, without an account and without internet access.

## Features
- **Week calendar** for the lectures of a semester: swipe between weeks, pinch to zoom, the current time is marked
- **Semesters and courses** with recurring events (lecture, exercise, lab, tutorial), weekly or every n weeks
- **In person, online and hybrid** events with room, meeting link, Moodle link and lecturers
- **Flexible edits** for a single session, this and all following sessions, or the whole series
- **Reminders** before every event, like a calendar app
- **Backup** of all data to a file, optionally protected with a password
- **Light and dark theme**, tablet layout support

UniPensum has no internet access. 
The only permissions it asks for are the ones for the reminders.

## Translations
UniPensum is available in English and German, and more languages are welcome. 
English is the fallback, so a translation can be sent in parts.

Is your language missing or a text wrong? 
[Open an issue](https://github.com/MCmoderSD/UniPensum/issues/new) or contribute the translation yourself:
1. Copy `app/src/main/res/values/strings.xml` to `app/src/main/res/values-<code>/strings.xml`, for example `values-fr`
2. Translate the texts, and leave names, placeholders (`%1$s`, `%d`) and `translatable="false"` entries as they are
3. Open a pull request

Nothing else has to change. 
The language is picked in the Android settings, per app on Android 13 and newer.

## Feature Requests & Contributing
Missing a feature or found a bug? [Open an issue](https://github.com/MCmoderSD/UniPensum/issues/new) and describe what you need.
Contributions are welcome too: fork the repository, make your change and open a pull request.
For a bigger change, open an issue first so we can talk about it before you start.

## Release
UniPensum will be published on [GitHub Releases](https://github.com/MCmoderSD/UniPensum/releases) and in the Google Play
Store. Both are coming soon.

## Project Structure
The app is written in Java with the classic Android View system. 
All data is stored in a local SQLite database.

```
de.mcmodersd.unipensum
├── domain              Plain Java without Android imports, covered by unit tests
│   ├── model           Semester, Course, Series, Session and their value types
│   ├── logic           Recurrence, semester rules, week layout, series editing, reminders
│   ├── text            Cleaning and checking of typed text, web links, e-mail addresses and phone numbers
│   └── backup          What a backup holds, and the checks that make imported data safe
├── data                Repository, settings and the read models of the UI
│   ├── db              SQLite schema, migrations and data access
│   └── backup          The .unipensum file: zip container, JSON, encryption, export and import
├── reminder            The alarm for the next reminder, and the notification
└── ui
    ├── week            The week grid, the app's main screen
    ├── session         Session details, edit scope and the event form
    ├── course          Course editor and course list
    ├── lecturer        Lecturer list, picker and editor
    ├── semester        Semester list and editor
    ├── settings        Theme, visible hours, reminders and how lecturers are named
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