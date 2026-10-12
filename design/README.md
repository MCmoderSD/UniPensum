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