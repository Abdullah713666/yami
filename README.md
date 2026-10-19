# Yami

An Android app that helps you stop doomscrolling Instagram Reels. **Shahid Anwar**, a pixel-art Pakistani police officer, sits on top of the Reels feed, counts every reel you swipe past, and gets angrier the further you go past the daily limit you set yourself.

<!-- screenshots -->

## What it does

- **Counts reels reliably**, including fast flicks. The counter tracks the pager's scroll offset instead of waiting for it to settle, so rapid swipes are not lost.
- **Shahid reacts to your count.** Relaxed under the limit, a whistle at the limit, then annoyed, angry and furious as you go over. A badge on him shows today's count against the limit.
- **Break card.** Every N reels past the limit a full-screen "Go touch grass" duty citation blocks the feed for a few seconds. It never hard-blocks.
- **Sound at the limit.** A police whistle or siren plays (up to 5 s, with a fade) the moment you cross the limit. You can also pick your own MP3, WAV, M4A, OGG or MP4.
- **Your own words.** Add, edit and switch off the break-card messages and Shahid's speech-bubble lines from the Messages screen.
- **You can play with him.** Drag him anywhere, throw him and he tumbles and bounces off the screen edges, gets dizzy with stars, dusts himself off and climbs back. Tap him to get a salute and today's count. He patrols, twirls his mustache and falls asleep on duty.
- **Private.** No internet permission, no analytics. Only the screen structure of Instagram is inspected; no content is read or stored.

## How it works

| Piece | Where |
|---|---|
| Accessibility service that watches Instagram and draws the overlay (`TYPE_ACCESSIBILITY_OVERLAY`, so no draw-over-apps permission) | `service/ReelWatcherService.kt` |
| Detects whether the Reels pager is on screen | `service/ReelsScreenDetector.kt` |
| Pure counting logic (scroll offset accumulation, unit tested) | `core/ReelSwipeTracker.kt` |
| Mood and message rules | `core/MoodEvaluator.kt`, `core/Messages.kt` |
| Pixel-art engine: 44x44 sprite stage, frame-based actions, unfiltered scaling | `character/ShahidView.kt` |
| Hand-drawn sprite data | `character/ShahidSprites.kt` |
| Drag, throw, bounce, patrol physics | `overlay/OverlayController.kt` |
| Speech bubbles and the break card | `overlay/BubbleView.kt`, `overlay/TouchGrassCard.kt` |
| Limit sound | `sound/SoundPlayer.kt` |
| Settings and stats UI | `ui/` |

Design intent is documented in [PRODUCT.md](PRODUCT.md) and [DESIGN.md](DESIGN.md): a "duty register" look in khaki, brass and challan red, in dark and light mode.

## Build

Kotlin, Views + Material 3, minSdk 24, target 37. No third-party dependencies beyond AndroidX and Material.

```bash
./gradlew testDebugUnitTest   # unit tests
./gradlew installDebug        # install on a connected device
```

After installing, open Yami, follow the prompt to enable its accessibility service (Settings > Accessibility > Installed apps > Yami), then open Instagram Reels.

Reinstalling the app can switch the accessibility service off on some phones; turn it back on if Shahid does not appear.

## Limits and notes

- Instagram Reels only for now. The code is structured around a `SupportedApp` type so YouTube Shorts and TikTok can be added.
- Counting depends on Instagram's view IDs (`clips_viewer_view_pager`), so an Instagram update can break detection until the ID is updated.
- Tested on a Samsung Galaxy A07 (720 x 1600, Android) in dark mode.
