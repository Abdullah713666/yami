# DESIGN: Yami (The Duty Register)

> "A tired citizen sitting on the edge of the bed in a dimly-lit bedroom at 11:45 PM, having just caught themselves in a 40-minute Instagram Reels trance, opening Yami to see Shahid Anwar's stern yet comradeship-filled gaze looking back from the duty register."

## Register & Anti-References
- **Register**: Product (Android utility serving doomscroll prevention).
- **Anti-References**:
  - No purple/indigo/violet primaries or gradients (banned AI default).
  - No mint/teal wellness or mindfulness aesthetics.
  - No giant hero-metric numbers with floating stat chips.
  - No identical card grids with repeated icon + heading + body.
  - No side-stripe borders or glassmorphism.
  - No em dashes in copy.
- **Physical Metaphor**: Shahid Anwar's pocket duty ledger and challan book. Official, crisp, warm, disciplined, with pixel-art integrity.

---

## Themes & Color Strategy
**Color Strategy**: Restrained (Tinted paper/slate neutrals with brass badge & challan red accents, <= 10% accent).

### Complete Material 3 Token Mapping
All Material 3 color roles are explicitly mapped to yami tokens in both themes:
- `colorPrimary` -> `@color/yami_primary`
- `colorOnPrimary` -> `@color/yami_on_primary`
- `colorPrimaryContainer` -> `@color/yami_primary_container`
- `colorOnPrimaryContainer` -> `@color/yami_on_primary_container`
- `colorSecondary` -> `@color/yami_brass`
- `colorOnSecondary` -> `@color/yami_on_primary`
- `colorSecondaryContainer` -> `@color/yami_brass_container` (Checked segments and selected tab background)
- `colorOnSecondaryContainer` -> `@color/yami_on_brass_container` (Checked segments and selected tab text)
- `colorTertiary` -> `@color/yami_khaki`
- `colorOnTertiary` -> `@color/yami_on_primary`
- `colorTertiaryContainer` -> `@color/yami_primary_container`
- `colorOnTertiaryContainer` -> `@color/yami_on_primary_container`
- `colorError` -> `@color/yami_challan_red`
- `colorOnError` -> `@color/white`
- `colorErrorContainer` -> `@color/yami_challan_red_container`
- `colorOnErrorContainer` -> `@color/yami_on_challan_red_container`
- `colorSurface` -> `@color/yami_surface`
- `colorOnSurface` -> `@color/yami_ink`
- `colorSurfaceVariant` -> `@color/yami_surface_subtle`
- `colorOnSurfaceVariant` -> `@color/yami_text_secondary`
- `colorSurfaceContainerLowest` -> `@color/yami_surface_lowest`
- `colorSurfaceContainerLow` -> `@color/yami_surface_low`
- `colorSurfaceContainer` -> `@color/yami_surface`
- `colorSurfaceContainerHigh` -> `@color/yami_surface_subtle`
- `colorSurfaceContainerHighest` -> `@color/yami_surface_highest`
- `colorSurfaceInverse` -> `@color/yami_surface_inverse`
- `colorOnSurfaceInverse` -> `@color/yami_on_surface_inverse`
- `colorOutline` -> `@color/yami_stroke`
- `colorOutlineVariant` -> `@color/yami_stroke_strong`
- `android:colorBackground` -> `@color/yami_bg`
- `colorControlHighlight` -> `@color/yami_highlight`

### Light Mode ("Day Shift / Duty Ledger")
- `yami_bg`: `#F5EFEB` (Aged warm parchment desk)
- `yami_surface`: `#FCFAF6` (Duty register sheet)
- `yami_surface_subtle`: `#EFE9DF` (Input boxes, chip troughs)
- `yami_stroke`: `#DDD5C7` (Ledger ruling lines)
- `yami_stroke_strong`: `#B8AC98` (Active stepper borders)
- `yami_ink`: `#1B1713` (Carbon black ink; contrast 16.6:1 on surface)
- `yami_text_secondary`: `#685E50` (Graphite pencil notes; contrast 5.8:1 on surface)
- `yami_text_tertiary`: `#8C8171` (Quiet ledger numbering; contrast 3.6:1)
- `yami_primary`: `#524328` (Dark uniform khaki; contrast 8.8:1 on surface)
- `yami_on_primary`: `#FAF7F2` (Crisp ivory on button)
- `yami_brass`: `#8F6715` (Polished brass badge)
- `yami_brass_container`: `#F7ECCF`
- `yami_on_brass_container`: `#332402`
- `yami_challan_red`: `#A32E22` (Lanyard red & violation stamp; contrast 6.8:1)
- `yami_challan_red_container`: `#FBE7E5`
- `yami_on_challan_red_container`: `#420E08`
- `yami_chart_normal`: `#5C7444` (Olive khaki bars)
- `yami_chart_over`: `#A32E22` (Challan red over-limit bars)
- `yami_chart_dash`: `#8C8171`

### Dark Mode ("Night Patrol / Field Notebook")
- `yami_bg`: `#15120F` (Night duty table; never pure #000)
- `yami_surface`: `#201C18` (Night ledger sheet; 22% lightness)
- `yami_surface_subtle`: `#2B2620` (Elevated controls / field backgrounds)
- `yami_stroke`: `#352F27` (Night ruled lines)
- `yami_stroke_strong`: `#4E4539` (Active borders)
- `yami_ink`: `#EDE7DD` (Ivory chalk ink; contrast 13.5:1 on surface)
- `yami_text_secondary`: `#A69C8E` (Night graphite; contrast 6.3:1 on surface)
- `yami_text_tertiary`: `#7D7364` (Quiet rules)
- `yami_primary`: `#D8AB46` (Luminous brass badge; contrast 8.3:1 with dark text)
- `yami_on_primary`: `#1F1706` (Deep carbon on brass)
- `yami_brass`: `#D8AB46`
- `yami_brass_container`: `#3D3018`
- `yami_on_brass_container`: `#F5DE96`
- `yami_challan_red`: `#E05D4D` (Warning crimson; contrast 4.6:1)
- `yami_challan_red_container`: `#451712`
- `yami_on_challan_red_container`: `#F8CCC7`
- `yami_chart_normal`: `#829E65`
- `yami_chart_over`: `#E05D4D`
- `yami_chart_dash`: `#7D7364`

### Touch-Grass Overlay Theme (Constant across system mode)
- `yami_break_scrim`: `#E615120F` (90% dim warm backdrop)
- `yami_break_card`: `#FBF8F2` (Parchment citation slip)
- `yami_break_ink`: `#1B1713`
- `yami_break_sub`: `#685E50`
- `yami_break_wait_bg`: `#E8E0D2` (Physical disabled button plate)
- `yami_break_wait_text`: `#383126` (Readable graphite: contrast 7.8:1 against wait_bg)
- `yami_break_out_bg`: `#A32E22` (Challan red CTA)
- `yami_break_out_text`: `#FFFFFF`
- `yami_break_more_text`: `#685E50`

---

## Typography Scale (System Fonts Only)
| Role | Size | Weight | Font Family | Usage |
|---|---|---|---|---|
| `TextAppearance.Yami.Display` | 36sp | Bold | `sans-serif` | Today's tally count |
| `TextAppearance.Yami.Title` | 18sp | Bold | `sans-serif` | Section titles, modal headers |
| `TextAppearance.Yami.Subheading` | 15sp | Medium | `sans-serif-medium` | Setting row labels, message titles |
| `TextAppearance.Yami.Body` | 14sp | Regular | `sans-serif` | Explanations, helper text |
| `TextAppearance.Yami.Caption` | 12sp | Regular | `sans-serif` | Subtitles, character counters |
| `TextAppearance.Yami.Stamp` | 11sp | Bold | `monospace` | Official duty status badge, ledger section labels |

---

## Spacing & Insets Architecture
- **System Inset Architecture**:
  - `ViewCompat.setOnApplyWindowInsetsListener` applied on the root container.
  - Root container `top padding = statusBars.top`, background = `@color/yami_bg`.
  - Nested scroll view begins below the status bar so scrolling content is clipped at the status bar line and never draws behind status icons.
  - Scroll view `clipToPadding = false` with `bottom padding = navigationBars.bottom`, allowing content to scroll behind transparent navigation bars cleanly.
- **Spacing Scale**:
  - `spacing_xxs`: 2dp
  - `spacing_xs`: 4dp
  - `spacing_sm`: 8dp
  - `spacing_md`: 12dp
  - `spacing_lg`: 16dp
  - `spacing_xl`: 20dp
  - `spacing_xxl`: 24dp
  - `spacing_section`: 28dp

---

## Shape Language & Section Composition
- **Max Two Framed Surfaces**:
  1. The Duty Inspection Header (`header_sheet`): framed card holding 232dp Shahid, single-glance count/limit tally, quota gauge, and status line.
  2. The Inspector Playground Drill Sheet (`playground_card`): framed card holding mood/action chips and reset button.
- **Ledger Sections**:
  - All other sections (Duty Limits, Police Alert Audio, Duty Messages, Patrol Officer, Last 7 Days) sit directly on `yami_bg`.
  - Each begins with a small section label (monospace, all-caps, letter-spacing 0.08, `yami_text_secondary`).
  - Followed by a 1dp `yami_stroke` rule, with rows separated by hairline rules.
  - Varied vertical rhythm: generous space before section labels (`spacing_section = 28dp`) vs compact row spacing (`spacing_sm = 8dp`).

---

## Information Architecture & Components

### Main Screen (R1)
1. **Duty Header** (Framed Card 1 of 2):
   - Centered 232dp x 232dp `ShahidView` (Shahid body renders ~126dp wide). Tap triggers `playWave()`.
   - Reduced empty padding so header height grows by at most 40dp.
   - Inspection plaque: `today_count` / `today_limit_target` in one glance (`14 / 30 reels today`).
   - Quota progress indicator mapped to officer mood.
   - One status line: "On duty in Instagram Reels • %d remaining" or over-limit state.
2. **Setup Prompt**:
   - Compact notice slip visible only when Accessibility service is off.
3. **Duty Limits** (Ledger Section on `yami_bg`):
   - Monospace label + rule.
   - Daily limit stepper, Instagram switch + stepper, Break frequency stepper.
4. **Police Alert Audio** (Ledger Section on `yami_bg`):
   - Monospace label + rule + "Test sound" button.
   - Choice: Off / Whistle / Siren / My sound (MaterialButtonToggleGroup). Checked button is `yami_brass_container` with `yami_on_brass_container` text.
   - Document picker (`*/*`, audio/video), persistable URI permission grant, filename display.
   - 3 trigger switches: limit crossed, break card, every reel over.
5. **Duty Messages** (Ledger Section on `yami_bg`):
   - Monospace label + rule.
   - Row 1: Break card messages (N active) -> opens MessagesActivity (Break section).
   - Row 2: Shahid's custom lines (N lines) -> opens MessagesActivity (Lines section).
6. **Patrol Officer** (Ledger Section on `yami_bg`):
   - Monospace label + rule.
   - Switch: Show in Reels (`overlayEnabled`).
   - Size toggle: S / M / L.
7. **Last 7 Days** (Ledger Section on `yami_bg`):
   - Monospace label + rule.
   - `WeekBarsView` with restyled tokens directly on `yami_bg`.
8. **Inspector Playground** (Framed Card 2 of 2):
   - Framed drill sheet for previewing moods/actions without altering real count.
   - Reset today's count action with confirmation dialog.

### Messages Screen (R2)
- Top tab switcher ("Break messages" / "Shahid's lines"). Selected tab = `yami_brass_container` with `yami_on_brass_container` text.
- Top status bar inset on root with `yami_bg` background; list bottom inset with `clipToPadding=false`.
- Explicit accessibility label: Back button labeled "Back".
- Break messages: built-in (toggle only) + own (edit, delete, toggle), "Add message" with 80-char limit, minimum 1 enabled invariant, inline card title preview.
- Custom lines: helper explaining `%d` and limit takeover, 3 preset chips, add/edit/delete.

### Touch-Grass Citation Card (R3)
- Physical citation slip style.
- Full-screen dimmed backdrop.
- ShahidView in `BREAK` mood holding citation.
- Large title with selected message.
- Body text showing exact reels watched and overage (no em dashes).
- High-contrast readable disabled countdown button (`Wait Ns`: `#383126` text on `#E8E0D2` plate, 7.8:1 contrast).
