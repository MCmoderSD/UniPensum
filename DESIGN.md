---
name: UniPensum
description: A simple calendar for universities and lectures, set in ink on paper.
colors:
  accent: "#0A0A0B"
  text_primary: "#0A0A0B"
  bg: "#FFFFFF"
  sheet: "#FFFFFF"
  text_on_accent: "#FFFFFF"
  surface: "#F4F4F5"
  divider: "#E4E4E7"
  track_off: "#8E8E96"
  text_secondary: "#6B6B72"
  text_detail: "#5A5A60"
  danger: "#CE2B37"
  accent-night: "#F5F5F6"
  text_primary-night: "#F5F5F6"
  bg-night: "#000000"
  sheet-night: "#121214"
  text_on_accent-night: "#000000"
  surface-night: "#1E1E21"
  divider-night: "#26262A"
  track_off-night: "#6B6B72"
  text_secondary-night: "#8F8F98"
  text_detail-night: "#8F8F98"
  danger-night: "#FF5A64"
  course_red: "#E5383B"
  course_orange: "#F77F00"
  course_yellow: "#D99A00"
  course_green: "#2D9D5B"
  course_teal: "#14909B"
  course_blue: "#2563EB"
  course_violet: "#7C3AED"
  course_pink: "#DB2777"
  course_graphite: "#2B2B30"
  course_gray: "#6B6B72"
  course_silver: "#A8A8B0"
  course_red-night: "#FF5A64"
  course_orange-night: "#FF9F43"
  course_yellow-night: "#FFD43B"
  course_green-night: "#3DD68C"
  course_teal-night: "#2BD4D0"
  course_blue-night: "#5B8DEF"
  course_violet-night: "#A78BFA"
  course_pink-night: "#F472B6"
  course_graphite-night: "#E6E6EA"
  course_gray-night: "#9A9AA3"
  course_silver-night: "#5E5E66"
typography:
  display:
    fontFamily: "Roboto, sans-serif"
    fontSize: "28sp"
    fontWeight: 700
    letterSpacing: "-0.01em"
  title:
    fontFamily: "Roboto, sans-serif"
    fontSize: "20sp"
    fontWeight: 500
  body:
    fontFamily: "Roboto, sans-serif"
    fontSize: "16sp"
    fontWeight: 400
  button:
    fontFamily: "Roboto, sans-serif"
    fontSize: "16sp"
    fontWeight: 500
  caption:
    fontFamily: "Roboto, sans-serif"
    fontSize: "14sp"
    fontWeight: 400
  segment:
    fontFamily: "Roboto, sans-serif"
    fontSize: "14sp"
    fontWeight: 500
  section:
    fontFamily: "Roboto, sans-serif"
    fontSize: "13sp"
    fontWeight: 500
  small:
    fontFamily: "Roboto, sans-serif"
    fontSize: "13sp"
    fontWeight: 400
  weekday:
    fontFamily: "Roboto, sans-serif"
    fontSize: "11sp"
    fontWeight: 500
    letterSpacing: "0.06em"
  hour:
    fontFamily: "Roboto, sans-serif"
    fontSize: "10sp"
    fontWeight: 400
  block-title:
    fontFamily: "Roboto, sans-serif"
    fontSize: "11.5sp"
    fontWeight: 500
  block-detail:
    fontFamily: "Roboto, sans-serif"
    fontSize: "10sp"
    fontWeight: 400
  wheel:
    fontFamily: "Roboto, sans-serif"
    fontSize: "22sp"
    fontWeight: 400
rounded:
  handle: "2dp"
  block: "6dp"
  control: "14dp"
  sheet: "28dp"
  full: "9999dp"
spacing:
  space_1: "4dp"
  space_2: "8dp"
  space_3: "12dp"
  space_4: "16dp"
  space_5: "24dp"
  space_6: "32dp"
components:
  button-primary:
    backgroundColor: "{colors.accent}"
    textColor: "{colors.text_on_accent}"
    typography: "{typography.button}"
    rounded: "{rounded.control}"
    height: "52dp"
  button-secondary:
    backgroundColor: "{colors.surface}"
    textColor: "{colors.text_primary}"
    typography: "{typography.button}"
    rounded: "{rounded.control}"
    height: "52dp"
  button-destructive:
    backgroundColor: "{colors.surface}"
    textColor: "{colors.danger}"
    typography: "{typography.button}"
    rounded: "{rounded.control}"
    height: "52dp"
  icon-button:
    textColor: "{colors.text_primary}"
    size: "48dp"
  text-field:
    backgroundColor: "{colors.surface}"
    textColor: "{colors.text_primary}"
    typography: "{typography.body}"
    rounded: "{rounded.control}"
    height: "52dp"
  row:
    backgroundColor: "{colors.surface}"
    textColor: "{colors.text_primary}"
    typography: "{typography.body}"
    rounded: "{rounded.control}"
    height: "56dp"
  segmented-control:
    backgroundColor: "{colors.surface}"
    rounded: "{rounded.control}"
  segment-selected:
    backgroundColor: "{colors.accent}"
    textColor: "{colors.text_on_accent}"
    typography: "{typography.segment}"
    rounded: "{rounded.control}"
    height: "48dp"
  segment-idle:
    textColor: "{colors.text_secondary}"
    typography: "{typography.segment}"
    height: "48dp"
  switch-on:
    backgroundColor: "{colors.accent}"
    rounded: "{rounded.full}"
    width: "52dp"
    height: "32dp"
  switch-off:
    backgroundColor: "{colors.track_off}"
    rounded: "{rounded.full}"
    width: "52dp"
    height: "32dp"
  sheet:
    backgroundColor: "{colors.sheet}"
    rounded: "{rounded.sheet}"
  session-block:
    textColor: "{colors.text_primary}"
    typography: "{typography.block-title}"
    rounded: "{rounded.block}"
  agenda-card:
    textColor: "{colors.text_primary}"
    typography: "{typography.body}"
    rounded: "{rounded.control}"
  today-marker:
    backgroundColor: "{colors.accent}"
    textColor: "{colors.text_on_accent}"
    rounded: "{rounded.full}"
  zoom-indicator:
    backgroundColor: "{colors.accent}"
    textColor: "{colors.text_on_accent}"
    rounded: "{rounded.full}"
---

# Design System: UniPensum

## Overview

**Creative North Star: "The Ink Timetable"**

UniPensum looks like a timetable printed in black ink on white paper, and at night like the same page in white on
black. The interface has no color of its own. Every hue on screen belongs to a course the student picked, so the week
reads at a glance as a pattern of subjects, not as an app. The chrome steps back: a bold semester name, a few quiet
icons, hairline grid lines, and soft gray cards for everything the hand touches.

The density follows the two jobs of the app. The week grid is dense on purpose, small type and tight blocks, because a
whole week has to fit on a phone between two lectures. Forms, details and settings are the opposite: large rounded
cards, generous height, one thing per row. Controls feel tactile and direct. Every tap, confirmation, refusal and toggle
answers with a haptic tick, presses dim the control instead of spreading a ripple, and motion is short and decisive.

Nothing in the app is decorative. There are no illustrations, mascots, confetti or gradients; the only picture is the
timetable itself.

**Key Characteristics:**
- Monochrome Ink and Paper chrome; course colors are the only hues, plus Danger for destructive actions.
- Flat: no shadows anywhere, depth from three tonal steps and the rounded top of sheets.
- One family (Roboto through the system `sans-serif`) in regular and medium, bold only for the semester name.
- Soft 14dp cards for every control; 6dp corners for the small blocks of the week grid.
- Tactile feedback: haptics on every interaction, opacity for pressed and disabled states, 160 to 260 ms motion.
- Light and dark are two equal schemes, mirrored token for token.

## Colors

A grayscale ink-and-paper palette that leaves every hue to the courses.

All colors live in `res/values/colors.xml` with a twin of the same name in `res/values-night/colors.xml`; the keys
above with `-night` are those twins. The resources store them as `#FFRRGGBB` (opaque ARGB); the frontmatter writes the
same colors as `#RRGGBB`.

### Primary
- **Ink** (`accent`, `text_primary`): the color of the app. Primary text, the filled primary button, the selected
  segment, the switch track when on, the today circle, the current-time line, the focus stroke of a field, the ring of
  the selected course color. Near-black on the light theme, near-white on the dark one.
- **Paper** (`bg`, `text_on_accent`): the window background and the text that sits on Ink. Pure white on the light
  theme, pure black on the dark one.

### Neutral
- **Sheet Paper** (`sheet`): the background of bottom sheets. The same white as Paper in light, a lifted
  near-black (#121214) in dark.
- **Card Gray** (`surface`): the fill of every control card: buttons other than primary, fields, rows, segmented
  controls, steppers.
- **Hairline Gray** (`divider`): grid lines, the sheet handle, disabled days in the month calendar. A line, never the
  shape of a control.
- **Track Gray** (`track_off`): the track of a switch that is off, at 3:1 against the page, so the control is seen
  without its label (light #8E8E96, dark #6B6B72).
- **Pencil Gray** (`text_secondary`): secondary text, section labels, hour labels, weekday letters, idle segments,
  chevrons, values at the end of a row.
- **Detail Gray** (`text_detail`): the small lines of a session, on the tint of a course color. Darker than Pencil
  Gray in the light theme (#5A5A60), because a tint is darker than the page; 4.5:1 on all eleven tints. In the dark
  theme it is the same as Pencil Gray.

### Course Colors
Eleven presets a student picks per course (`CourseColor`): eight saturated hues (red, orange, yellow, green, teal,
blue, violet, pink) and a grayscale ramp (graphite, gray, silver). Each has its own night value, brighter and lighter
so it holds on black; graphite and silver swap brightness between the themes.

### Signal
- **Danger** (`danger`): destructive actions and errors only: the text of the destructive button, the stroke and
  message of a field with an error. 4.75:1 on Card Gray in the light theme (#CE2B37).

### Named Rules
**The Ink Rule.** The interface has no hue of its own. Chrome is Ink, Paper and grays; color enters only through a
course and, for destruction and errors, through Danger. A colored brand primary, a tinted header or a colored icon
breaks the system.

**The Mirror Rule.** Every color resource has a night twin of the same name. Ink and Paper swap places between the
themes; nothing is inverted automatically and nothing is left without a dark value.

**The Tint Rule.** A course color fills an area only as an 18% tint (alpha `0x2E`) and appears at full strength only
as a line or a dot: the 3dp bar of a session block, the dot in a row, the swatch.

**The Contrast Rule.** Text is 4.5:1 and the shape of a control, its state and its focus mark are 3:1 against what
they sit on, in both themes. A new color token is measured before it is used; on a course tint the text color is
`text_detail`, not `text_secondary`.

## Typography

**Display Font:** Roboto, through the platform's `sans-serif`
**Body Font:** Roboto (`sans-serif`, and `sans-serif-medium` for the medium weight)

**Character:** One sober family in two working weights. Regular carries content, medium carries anything that labels
or acts, and bold is kept for the semester name at the top of the week.

### Hierarchy
- **Display** (bold 700, 28sp, letter spacing -0.01em): the semester name on the week screen, the one bold line in the
  app.
- **Title** (medium 500, 20sp): sheet titles and the title of the top bar on sub-pages.
- **Body** (regular 400, 16sp): row titles, field input, detail values, the value of a stepper.
- **Button** (medium 500, 16sp): the label of every button.
- **Caption** (regular 400, 14sp, Pencil Gray): the date range and week number under the semester name.
- **Segment** (medium 500, 14sp): the options of a segmented control.
- **Section** (medium 500, 13sp, Pencil Gray): the heading above a group of fields, inset 4dp.
- **Small** (regular 400, 13sp, Pencil Gray): row subtitles, field labels, field error messages (in Danger).
- **Wheel** (regular 400, 22sp): the numbers of the time wheel.
- **Grid type**: weekday letters (medium 500, 11sp, uppercase, letter spacing 0.06em), day numbers (medium 500, 16sp),
  hour labels (regular 400, 10sp; the current hour in Ink, medium), block titles (medium 500, 11.5sp; 9.5sp in a
  narrow block), block details (regular 400, 10sp; 9sp narrow, in Detail Gray).

### Named Rules
**The One Family Rule.** The typeface is set once in `TextAppearance.Up` and nowhere else. A screen picks one of the
`TextAppearance.Up.*` styles; it does not choose its own size or font.

**The sp Rule.** Every text size is in sp, so type follows the system font size. Custom views convert with
`TypedValue.applyDimension(COMPLEX_UNIT_SP, ...)`, never with dp or px.

## Layout

**Spacing scale.** Six steps in `dimens.xml`: 4, 8, 12, 16, 24 and 32dp (`space_1` to `space_6`). Screens pad 16dp at
the sides; sheets start their title at 24dp; cards stack with small gaps of 8 to 12dp; a section label sits above its
group.

**Edge to edge.** The status and navigation bars are transparent and the app draws behind them, with insets applied
to the content.

**The week screen.** A header with the semester name (Display, with a chevron to switch semesters) and the date range
and week number (Caption) on the left, and three icon buttons on the right: add a course, the course list, the
settings. Below it the week pager. The grid shows Monday to Friday only, in five equal columns, behind a gutter of
hour labels. An hour is at least 44dp tall and grows with pinch zoom; the grid has 10dp of padding at top and
bottom, and each block is inset 1dp from its column. The gutter is 44dp, the day header 56dp tall; both grow with the
font size of the system, the gutter to the widest hour label and the header with the day numbers. The semester name
and the date line may take a second line instead of being cut off. With a keyboard the Tab key goes through the
header and then through the sessions of the week that is shown, and Page Up, Page Down and Home turn the weeks and
come back to this one. A screen reader hears the new week from the date line, which is a live region.

**The day list.** From a font scale of 1.3 on ("Largest" on most phones) five columns cannot hold the text, so the
week is a list: one heading per day (weekday and date, with "Today" on a line of its own above the current one), below it one
card per session or the line "No events". The cards are as wide as the screen and show every line in full. There is no grid, no day
header and no zoom in this mode. A person who needs the large text cannot ask for the grid back: that is a known
limit, and a setting for the week view would be the way out.

**Orientation.** The app turns on phones and tablets. In landscape a window of 720dp and more splits into week and
page, so a phone held sideways shows both. On a screen less than 480dp high the header is compact: the date range
stands beside the name of the semester, and the header is as high as its buttons. The grid of a phone of 640 x 360dp
then shows four and a half hours at a time and scrolls for the rest, and a sub-page covers the week there. For large
text, where the days are a list, the header keeps its two lines.

**Sub-pages and sheets.** A sub-page (settings, courses, editors) has a 56dp top bar with a back arrow and a Title.
The title is also the name of the page for a screen reader, which says it when the page opens ("Courses, Winter 26/27"
where the bar shows only the semester); the page itself only keeps touches from reaching the week behind it and is
not a stop of its own.
A sheet has a 36 x 4dp handle 12dp below its top edge, a title row of at least 60dp, a scrolling content area, and an
optional footer that stays in place (the Save button). A tall sheet stops 8dp below the status bar.

**Phone and tablet.** On a window narrower than 720dp, a page opened from the week covers it. From 720dp on,
`PaneLayout` places the week and the page side by side; the page takes a share of the width between 360 and 480dp.

**Right to left.** The layout runs in the direction of the system language, and the whole app follows it, the week
included. Monday is the rightmost day, the hours stand on the right, the bar of a session and the dot of the current time
sit on the start edge, and the month calendar and the color swatches begin on the right. The pane beside the week opens
from the left, and a page slides in from the left. A text starts at the start of its view (the theme sets
`textAlignment` to `viewStart`), so an English text in a mirrored row stands on the side of its neighbors. Two things
read from left to right in every language: a phone number, an e-mail address and a link, because their groups of
digits would come out reversed, and a time, hours before minutes, in the wheels and in the entry.

## Elevation & Depth

The system is flat. No view casts a shadow; even the bottom sheet has an elevation of 0dp. Depth comes from three tonal
steps and from shape: Paper is the floor, Sheet Paper is the layer that slides up (identical to Paper in light, lifted
to #121214 in dark), and Card Gray marks everything that can be touched. A sheet separates from the page by its
rounded top corners and by the system scrim behind it.

### Named Rules
**The Flat Rule.** No shadows, anywhere, in either theme. A new layer gets a tonal step or a shape, never an
elevation.

## Shapes

Soft but not round. Controls are cards with gently curved corners (14dp); the blocks of the week grid use small
corners (6dp) so many of them can sit close together; sheets open with large rounded top corners (28dp) and square
bottom ones. Circles are reserved for state: the today marker, the selected and the current day of the month
calendar, the course color swatches, the course dot in a row. Pills (full rounding) are used for the switch and the
zoom indicator; the sheet handle has 2dp corners. Nothing has a border at rest; strokes appear only for state (focus,
error, overlap, selection).

### Named Rules
**The Fourteen Rule.** Every control the hand touches is a Card Gray card with 14dp corners: buttons, fields, rows,
segmented controls, steppers. A new control follows it.

## Components

Every control is a UniPensum widget in `ui/widget` (`Up*`). Material Components is only the base for the bottom sheet
mechanics (dragging, predictive back); no Material widget is inflated. Pressed and disabled states change the opacity
of the control, and `Haptics` gives every interaction a haptic: a tick on tap, confirm and reject on success and
refusal, toggle on and off for switches.

### Buttons
Tactile slabs that fill the width they are given.
- **Shape:** gently curved (14dp), at least 52dp tall, label in Button type.
- **Primary:** Ink fill with Paper label. One per screen or sheet, for the most likely next action: Save in an
  editor, Confirm in a confirmation, and in the session sheet Open meeting when the session has a link, otherwise Edit.
- **Secondary:** Card Gray fill with Ink label: the other actions next to it (the course page link, Edit when Open
  meeting is primary, Cancel).
- **Destructive:** Card Gray fill with a Danger label (Delete). It is never filled with red.
- **States:** pressed 70% opacity, disabled 40%. Two buttons side by side split the width in equal halves.

### Icon Buttons
- **Style:** a 48 x 48dp target with a 24dp Material Symbols Rounded icon in Ink, no background.
- **States:** pressed 50% opacity, dimmed 30%, keyboard focus the Focus Ring.
- **Hold to repeat:** on a stepper, holding repeats the action after 160ms and speeds up by 12ms per step to every 50ms.

### Inputs / Fields
- **Style:** a Small label in Pencil Gray above a Card Gray field (14dp, at least 52dp tall) with Body text and a
  Pencil Gray hint. No stroke at rest. The label is the name of the field for a screen reader.
- **Focus:** a 1.5dp Ink stroke.
- **Error:** a 1.5dp Danger stroke and a Small message in Danger below the field. The message is a live region, so a
  screen reader says it when it shows, and the field carries it too. A form that refuses its input moves the focus to
  the first field it refused. A message that is shown again, as the answer to a second press on Save, is cleared and
  set again a moment later (`LiveMessage`), because a screen reader does not say the same text twice in a row.

### Rows
- **Style:** a Card Gray card (14dp, at least 56dp tall) with a Body title, an optional Small subtitle, an optional
  value in Pencil Gray at the end, an optional chevron, and an optional leading icon or course color dot.
- **State:** pressed 60% opacity. Detail sheets are stacks of rows, one fact per row (time, place, lecturer, e-mail,
  phone, reminder).

### Segmented Control and Stepper
- **Segmented control:** a Card Gray track (14dp) with segments 48dp tall, the touch size. The selected option is an
  Ink pill, drawn 3dp inside the segment, with a Paper label; the others are Pencil Gray. The pill slides to a new
  option in 260ms on the curve (0.2, 0, 0, 1). For a screen reader it is a group of radio buttons, "1 of 4", and
  every option says the name of the group: "Type: Lecture". Options that do not fit into one row, as with a large
  system font, stand in a column of 48dp rows instead of being cut off.
- **Stepper:** a Card Gray card (14dp) with minus and plus icon buttons and the value centered in Body type. The
  buttons say what they set ("Day starts at, decrease"), and a screen reader hears the new value after every change.

### Switch
- **Style:** a track of 52 x 32dp, a full pill, in a touch area of 52 x 48dp. Off: Track Gray with a Sheet Paper
  thumb. On: Ink track with a Paper thumb. The thumb is a circle 3dp inside the track and sits at the end of it when the
  switch is on.
- **Row:** the label and the switch are one row of at least 48dp, and a tap anywhere on it toggles the switch. The
  screen reader gets the switch alone, named after the row.
- **Motion:** the thumb and both colors move together in 160ms, with a toggle haptic.

### Focus Ring
Keyboard, D-pad and switch focus is shown by a 2dp Ink ring, as round as the control it sits on (`FocusRing`): 14dp
on buttons, rows and cards, 6dp on a session block, full on an icon button, 8dp on the semester name. It replaces
the gray square of the system. Controls take no focus from a touch, so the ring never shows after a tap.

A grid that draws its own cells carries the ring itself, on the cell that has the focus: a circle 3dp outside the day
in the month calendar, a rounded square around the cell in the color swatches. The arrow keys move between the cells,
Enter chooses one, and Tab leaves the grid; the focus starts on the chosen cell.

### Sheets
- **Style:** Sheet Paper, 28dp top corners, the handle, a Title, no elevation. Opened expanded.
- **Behavior:** dragged only by the handle and title, so scrolling and pickers inside never close it by accident;
  closed by dragging down, tapping outside or Back. A footer holds the primary action out of the scroll.

### Session Block (signature component)
The heart of the app: one session in the week grid.
- **Shape:** 6dp corners, inset 1dp in its column.
- **Color:** the course color as an 18% tint, with a 3dp bar in the full course color along the start edge.
- **Text:** the course name in block title type and Ink, then the details in block detail type and Detail Gray: time,
  room or mode ("A2 | Hybrid", "Online"), type, lecturer. Below 52dp of width the type steps down (9.5 and 9sp) and the
  padding tightens. The name is hyphenated by the language of the device, or by English for a name in Latin letters on a
  device in another script, Arabic or Hebrew, which has no patterns for it.
- **Overlap:** a session that overlaps another gets a dashed 1.5dp outline in its course color (4dp dashes, 3dp gaps).
- **State:** pressed 65% opacity; a tap opens the session sheet; keyboard focus the Focus Ring.
- **Screen reader:** "Tuesday, Oct 13, Algorithms, Lecture, 8:00 AM – 11:15 AM": the day comes first, because the
  week header is not read.

### Day Card (signature component of the day list)
The same card as the block, as wide as the screen: the 18% course tint with a 4dp bar, 14dp corners, the course name in
Body medium and Ink, then time, place, type and lecturer in Caption and Detail Gray, each on its own row and never cut
off. Pressed 65% opacity, tap opens the session sheet. Its heading is the day, which a screen reader can jump to.

### Week Header and Now Line
- **Week header:** uppercase weekday letters over day numbers; today's number sits in an Ink circle with a Paper digit.
  Its height grows with the day numbers when the system font is larger.
- **Now line:** a faint line (40%, thin) across the whole week at the current time, and in today's column a 2dp Ink
  line. Both stop at a session block, so they never strike through the text of the lecture that is on; where the time
  falls into a block, an Ink dot at its start edge marks it. The hour label of the current hour turns Ink.

### Pickers
- **Color swatches:** six columns of course color circles; the selected one gets a 2.5dp Ink ring 5dp outside it.
- **Month calendar:** 48dp day cells; the selected day is an Ink circle with a Paper number, today a 1.5dp Ink ring;
  disabled days in Hairline Gray.
- **Time wheel:** five visible values in Wheel type, with a keyboard entry beside it. It is the one control of the app
  with an iOS look; the keyboard entry keeps it reachable for everyone. The two number fields of the entry carry their
  names, Hour and Minute, in Small type below them.
- **Zoom indicator:** an Ink pill (36dp, in a 48dp touch area) with a Paper label that shows the zoom level while
  pinching and fades once the zoom is back at 100%.

### Icons
Material Symbols Rounded, 24dp, as vector drawables (960-unit viewport), tinted Ink, or Pencil Gray for chevrons in
rows. Going back is an arrow; the chevron points further into the app (rows, months, the semester menu).

### Decisions kept on purpose
Dynamic Color is not used: the Ink Rule keeps the chrome without a hue of its own. A confirmation opens as a sheet,
not as a dialog, like everything else that is a short task of its own. Pressing dims a control instead of spreading a
ripple.

A session shorter than 48dp stays as short as its time: the height of a block in the grid is the length of the
session, which a time grid says by its shape, so the block is smaller than a touch target. The list of days, from a
font scale of 1.3, gives every session a card of full height.

## Do's and Don'ts

### Do:
- **Do** take every color from a `@color/` resource and give each new one a twin in `values-night/colors.xml`.
- **Do** keep the course color the only hue on a screen: an 18% tint for areas, full strength for bars, dots and
  swatches.
- **Do** build a new control as an `Up*` widget in `ui/widget`: Card Gray, 14dp corners, at least 52dp tall when it
  spans the width.
- **Do** set text through the `TextAppearance.Up.*` styles, in sp.
- **Do** answer every tap, confirmation, refusal and toggle with `Haptics`, show pressed and disabled states with
  opacity, and keep motion between 160 and 260ms on the (0.2, 0, 0, 1) curve.
- **Do** keep one Ink-filled primary button per screen or sheet, for the most likely next action.
- **Do** meet WCAG 2.2 AA contrast for text in both themes, including Detail Gray text on tinted session blocks, and
  3:1 for the shape and state of a control (The Contrast Rule).
- **Do** give every control a touch target of at least 48dp, even where it draws less, and the Focus Ring wherever it
  can take focus.
- **Do** make a custom control say what it is to a screen reader: a role, its state, its name, and the day on a
  session; mark titles and sections as headings.
- **Do** name every field with its label view (`labelFor`), make every message that appears after an action a live
  region that is shown through `LiveMessage`, so that it is said again when it is shown again, and give every page
  that opens a pane title.
- **Do** let a keyboard do what a finger does: every control a stop of the Tab key with the Focus Ring, a grid that draws
  its cells with arrow keys and Enter.
- **Do** let a text grow: use sp, let a line wrap, and size what holds text from the text.
- **Do** give every padding and margin that is not the same on both sides a start and an end, never a left and a right,
  and let a view that draws its own columns ask `Mirror`. A view that centers its text sets `textAlignment` to
  `center` as well as the gravity, because the alignment wins over the gravity.

### Don't:
- **Don't** add shadows or elevation to any view (The Flat Rule).
- **Don't** give the chrome a hue: no colored primary, no tinted top bar, no colored icons outside course colors and
  Danger (The Ink Rule).
- **Don't** inflate Material widgets or let Material's default colors show; Material only drives the sheet mechanics.
- **Don't** use illustrations, mascots, confetti or gradients anywhere in the app.
- **Don't** fill a destructive button with red; Danger is a text and stroke color.
- **Don't** write a color as a hex value in Java or a layout, or a text size in dp or px.
- **Don't** draw a solid line or any mark through the text of a session.
- **Don't** lock the orientation, or cut a text off where it can wrap.
