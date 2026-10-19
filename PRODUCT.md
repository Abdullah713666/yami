# Yami (product context)

register: product

## Product purpose
Yami is a personal Android app that helps one person stop doomscrolling Instagram Reels. It counts every reel they swipe to and puts a pixel-art Pakistani police officer, **Shahid Anwar**, on top of the Reels feed. Under the daily limit he is relaxed and funny; at the limit he blows his whistle; past it he gets annoyed, angry, then furious, and every few reels over the limit he blocks the feed with a break card ("Go touch grass") that takes a few seconds to dismiss.

The app screen itself is where the person sets their limits, the sound, the break messages and Shahid's lines, and sees today's and the last 7 days' counts. They open it a few times a week, usually right after a scrolling session, often at night, on a 720 x 1600 Samsung phone in dark mode (light mode must also work).

## Users
One person (the owner), Pakistani, bilingual English / Roman Urdu, technical, a little self-aware about their scrolling habit. They want the app to be funny and on their side, not preachy.

## Brand and tone
- Shahid Anwar is the brand. Dark khaki uniform, peaked cap with a brass badge, thick handlebar mustache, red lanyard, wooden lathi. Stern on the outside, soft on the inside. Desi humour: "Oye!", "challan", "Phone neeche!".
- Copy: short, direct, a bit cheeky, mixes English with light Roman Urdu. Never shames the user. Numbers are always concrete ("12 over your limit"), never vague.
- The app should feel like Shahid's own duty notebook / challan book: physical, warm, a bit official, with pixel-art moments. Not a wellness app, not a SaaS dashboard.

## Anti-references (do not look like these)
- Generic wellness / mindfulness apps: pastel mint, teal, lavender, soft blobs, "breathe" vibes.
- AI-made app clichés: purple-to-blue gradients, glassmorphism cards, neon on black, gradient text, a giant hero number with tiny label and stat chips, identical rounded card grids with an icon + heading + text each.
- Default Material 3 purple baseline theme.
- Duolingo copycat (bright green owl energy).

## Strategic principles
1. The character carries the personality; the UI frames him and stays clear.
2. Today's count vs the limit must be readable in one glance.
3. Every setting is one tap away on the main screen or one level deep, never more.
4. Works in dark and light mode with the same identity.
5. Pixel-art accents are welcome where they help (dividers, stamps, badges), but body text stays highly readable.

## Constraints
- Android, Kotlin, XML Views + Material Components 3 (already in the project). No Compose. No new dependencies, no network, no downloaded fonts or images: system fonts only (sans-serif family variants, serif, monospace, sans-serif-condensed).
- minSdk 24, target 37.
