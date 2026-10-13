# Design

## App icon
A mortarboard over five bars, one for each weekday of the timetable. Black and white, like the app.

| File                 | Use                                                                             |
|----------------------|---------------------------------------------------------------------------------|
| `icon-dark.svg`      | Source of the dark icon (black background, white mortarboard), the default icon |
| `icon-light.svg`     | Source of the light icon, shown on phones in light mode                         |
| `icon-dark-512.png`  | The icon for the Google Play listing: 512 × 512 px, 32-bit PNG, a full square   |
| `icon-light-512.png` | The same in the light variant                                                   |

The SVGs are made of separate parts with ids, so they can be edited or exported in any vector tool: `background`,
`mortarboard` (`mortarboard-top`, `mortarboard-body`) and `bars` (`bar-mon` to `bar-fri`). The drawing area is
130 × 130 units, which is the visible part of the Android icon.

Google Play shows one icon per app, the one uploaded to the store listing. It does not change with the theme.

### Android resources
The same shapes are kept as vector drawables, which have to be changed by hand together with the SVGs:

- `app/src/main/res/drawable/ic_launcher_foreground.xml`: the launcher icon, colors from `values/ic_launcher_colors.xml`
  (dark) and `values-notnight/ic_launcher_colors.xml` (light)
- `app/src/main/res/drawable/ic_splash.xml`: the splash screen, in the colors of the app theme

The 512 px files are rendered from the SVGs, for example with a browser or Inkscape at 512 × 512.

## Feature graphic
The wide picture of the Google Play listing: the name, the one-line description and the five bars of the week, in the
dark colors of the app. No cap, because Google Play does not want branding that repeats the app icon.

| File                           | Use                                                                             |
|--------------------------------|---------------------------------------------------------------------------------|
| `feature-graphic.svg`          | Source, made of parts with ids: `background`, `name`, `tagline`, `bars`, `days` |
| `feature-graphic-1024x500.png` | The file for the Play Console: 1024 × 500 px, 24-bit PNG without alpha          |

Colors: background `#0A0A0B`, name and bars `#F5F5F6`, tagline and weekday letters `#9A9AA3`. The bars have the heights
of the app icon. The text is set in Roboto, the font of the app, as live text: install Roboto to edit the SVG. The PNG
is rendered from the SVG at 1024 × 500, for example with a browser.

Rules of the Play Console (help page "Add preview assets", checked on 2026-10-13):
- 1024 × 500 px, JPEG or 24-bit PNG **without alpha**, key content away from the edges and near the center.
- Google advises against pure white, black or dark gray backgrounds, because they vanish into the Play background.
  This graphic is near black (like the dark icon), so on the dark Play theme its edge has no contour. If that is a
  problem, use the light variant: background `#E9E9EC`, name and bars `#0A0A0B`, tagline and letters `#55555C`.
- No device frames, store badges, third-party marks or branding that repeats the icon.
- If the listing has a promo video, Play may put a play button in the middle of the picture. The middle (about
  x 460 to 560) is empty for that reason; check the preview in the Play Console.
- An alt text of at most 140 characters is needed for the image.

Alt text (126 characters):

> The name UniPensum and the line A simple calendar for universities and lectures, next to five white bars for Monday to Friday.
