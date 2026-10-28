# 02 — Design System (colorful, smooth, AMOLED-first)

Goal: a premium, vibrant but clean player. Artwork and track titles are the heroes. Gradients create atmosphere; they are never behind every button.

## 1. Color tokens
```kotlin
object Pulse {
    // Surfaces (AMOLED first)
    val Black      = Color(0xFF000000)
    val Bg         = Color(0xFF07060C)
    val Surface    = Color(0xFF12101C)
    val Elevated   = Color(0xFF1B1829)
    val Outline    = Color(0xFF2E2A42)
    // Text
    val TextHi     = Color(0xFFFFFFFF)
    val TextMid    = Color(0xFFA0A0A0)   // secondary per original spec
    val TextLow    = Color(0xFF6E6B80)
    // Accents
    val Cyan       = Color(0xFF00C3FF)   // favorite heart + highlights (original spec)
    val Violet     = Color(0xFF9B7BFF)
    val Magenta    = Color(0xFFFF2A6D)
    val Pink       = Color(0xFFFF6DAA)
    val Coral      = Color(0xFFFF8C78)
    val Mint       = Color(0xFF3DDC97)
    val Orange     = Color(0xFFFF9500)   // APPLY button (original spec)
    val Purple     = Color(0xFFB36BFF)
    val Error      = Color(0xFFFF647C)
    // Original dark-magenta background spec
    val PlumTop    = Color(0xFF2A0826)
    val PlumBottom = Color(0xFF120310)
}
```

## 2. Gradient presets (used for skins and backdrops)
| Name | Colors (top → bottom) |
|---|---|
| Plum (default) | `#2A0826` → `#120310` → `#000000` |
| Aurora | `#3A1C71` → `#1B1B4B` → `#00C3FF33` over black |
| Afterglow | `#FF2A6D` → `#5B1A58` → `#0A0612` |
| Ocean | `#0F4C75` → `#051937` |
| Sunrise | `#FF8C78` → `#C2377A` → `#2B1055` |
| Forest | `#0B3D2E` → `#03140F` |
| Mono | `#2B2B2B` → `#000000` |
| Mesh (Player theme B) | `#FF2A6D` → `#051937` |
| Dynamic | dominant artwork color (Palette), darkened to HSL lightness ≤ 0.22, → black |

Rules: foreground text over any gradient must be white and keep contrast ≥ 4.5:1 (darken the gradient, never lighten text). Never use color alone for state; pair with an icon change or label.

## 3. Shape, spacing, type
- 4dp grid: 4, 8, 12, 16, 20, 24, 32.
- Screen horizontal padding **16dp** (20dp on width ≥ 400dp, 32dp on tablets). **Every** text and element sits inside this padding; sections separated by 24dp.
- Artwork radius 12dp (lists 10dp), cards 20dp, bottom sheets 28dp, primary buttons = full pill (`CircleShape`).
- Min touch target 48dp.
- Font: Roboto/system (or "Inter" only if bundled). Sizes: screen title 28sp Bold, section 20sp SemiBold, track title 16sp Bold, metadata 13sp Regular/Medium. Respect font scale.

## 4. Icons (Material icons-extended; replace every emoji)
Menu `Menu` · Skin `Checkroom`/`AutoAwesome` · Search `Search` · Settings `Settings` · Play `PlayArrow` · Pause `Pause` · Next `SkipNext` · Prev `SkipPrevious` · Shuffle `Shuffle` · Repeat `Repeat`/`RepeatOne` · Heart `Favorite`/`FavoriteBorder` · Queue `QueueMusic` · Add to playlist `PlaylistAdd` · Equalizer `Equalizer`/`Tune` · Sleep `Bedtime`/`Timer` · Lyrics `Lyrics` · Folder `Folder` · Album `Album` · Artist `Person` · Recently added `LibraryAdd` · Recently played `History` · Top tracks `LocalFireDepartment` · More `MoreVert` · Back `ArrowBack` · Collapse `KeyboardArrowDown` · Replay10 `Replay10` · Forward10 `Forward10` · Export `Upload` · Import `Download` · Check `Check` · Special badge `WorkspacePremium` · Fallback art `MusicNote`.

## 5. Motion spec
| Interaction | Animation |
|---|---|
| Tap feedback | ripple + `scale 0.96` spring (stiffness Medium) |
| Heart | scale 1 → 1.35 → 1 spring, color tween 200 ms |
| Play/Pause | `AnimatedContent` icon crossfade + scale 160 ms |
| Mini → Full player | `AnimatedContent` slide up 300 ms + artwork fade; backdrop color tween 400 ms |
| Tab change | pager swipe + indicator offset spring |
| Screen change | fade+slide 240 ms (`slideInHorizontally { it / 8 } + fadeIn`) |
| Lists | first-load stagger (max 12 items, 40 ms apart); never animate on scroll |
| Playing indicator | 3 animated bars (`rememberInfiniteTransition`), only on the current row while playing |
| Vinyl / disc | rotate 360° per 8 s while playing; pauses (keeps angle) when paused |
| Backdrop | cross-fade between old/new dynamic gradient 500 ms |

**Reduced motion:** a setting (and system animator scale = 0) disables disc rotation, bars, stagger and infinite animations. All features still work.

## 6. Responsive rules (use `WindowSizeClass`)
- Compact (<600dp): single column, drawer, floating mini player, full-screen Now Playing.
- Medium (600–839dp): NavigationRail, two-column For you widgets, album grid adaptive 160dp.
- Expanded (≥840dp): rail + content + docked Now Playing panel (width 380dp), songs list and queue side by side.
- Always keep artwork **square**; only grow columns, never stretch. Test widths 320, 393, 430, 600, 840, 1280 dp and font scale 1.0/1.3/2.0.
- Insets: `Modifier.windowInsetsPadding(WindowInsets.safeDrawing)` on root; mini player adds `navigationBarsPadding()`; the last list item gets bottom padding = mini player height + 16dp so nothing is hidden.

## 7. Theme modes
System / Dark (plum gradient) / AMOLED (pure black, no gradient except Now Playing) / Light (warm off-white `#FAF7FF`, white cards, text `#1A1625`, accents desaturated ~15%). Light mode is designed, not inverted.

## 8. Visual quality checklist
Consistent padding · no clipped text at 2.0 font scale · no gradient hurting contrast · mini player never covers last row · punch-hole camera area free of controls (top 24dp safe) · rounded screen corners respected (extra 4dp inner padding for edge-hugging elements).
