# Lap Counter: notes for Claude

A big-button walking lap counter for Android (Samsung S22 Ultra, also a Lenovo tablet).
The owner puts the phone down at one end of a short track (e.g. kitchen sink to door and back = 23.5 m),
taps one big LAP button each time they pass, and the app counts laps and distance in metres.

The owner doesn't edit code. They ask for changes in chat, often from the phone, and install the APK
from GitHub Releases. Explain results in plain language, not code.

## Layout

| Path | What it is |
|---|---|
| `app/src/main/assets/index.html` | The whole UI and all logic (single file, ~950 lines). Screens are `<section>`s: `home`, `setup`, `walk`, `summary`, `history`. |
| `app/src/main/java/ie/tenet71/lapcounter/MainActivity.java` | Full-screen WebView plus the `window.LapNative` bridge. |
| `app/build.gradle` | `versionName` is `1.0.<GITHUB_RUN_NUMBER>`. Signing config with the committed keystore. |
| `.github/workflows/build.yml` | Builds a signed APK on every push to `main` and publishes release `v1.0.N` as latest. |
| `app/lapcounter.keystore` | Fixed signing key. **Never change or regenerate it**, or updates won't install over the old app and the owner loses their history. |

## Native bridge (`window.LapNative`)

- `vibrate(patternJson)`, `speak(text)` (UK English TTS), `keepScreenOn(bool)`, `exitApp()` (`finishAndRemoveTask`).
- Back button: `MainActivity.onBackPressed` calls `window.lcBack()`. Return true to swallow it (e.g. during a walk).
- The page must still work with `NATIVE == null` (plain browser preview). Native-only UI like the ✕ stays hidden there.

## Storage

localStorage keys: `lc.settings` (track, goal, beep/voice), `lc.session` (walk in progress, so it can resume), `lc.history` (finished walks).
History lives only on the phone. Keep these keys and their shapes backward compatible, because updates must keep existing history.

## Behaviour the owner has decided (don't change without asking)

- Distance and laps are more prominent than time.
- The tally (laps + distance) is sized by `fitTally()` so 999 laps and 9999 m (plus the track's decimals) fit side by side with a 24 px gap. Sized once per screen/track so numbers don't jump.
- Walk screen shows "Avg speed" (km/h, completed laps' distance / their time), updated per lap; "–" before the first lap.
- Track length allows decimals (up to 4 digits + 2 dp, e.g. 23.5). Goal is whole metres (up to 5 digits).
  Keypad bottom row is `. 0 ⌫`. Hold ⌫ to clear.
- Lap distances are rounded to 2 dp (`mdist`, `r2`) so 3 × 23.5 shows 70.5.
- `MIN_LAP_MS = 5000`: taps within 5 s of the last lap are ignored (was 30 s, too long for a short track).
- Feedback: buzz minimum, plus beep and optional voice ("Lap 4. 100 metres."). Goal reached: long buzz + 3-note beep, keeps counting.
- Reset needs confirmation. Stop asks "Finish this walk?" then saves. Zero-lap walks aren't saved.
- ✕ close button on the **Home screen only**, no confirmation. Walking screens use Pause/Stop.
- Screen stays on during a walk. Portrait only. Only permission is VIBRATE, no internet.

## Making a change

1. `git pull --rebase origin main`, then edit.
2. Test `index.html` headless with Playwright (Chromium is preinstalled). Stub the bridge with
   `window.LapNative={vibrate(){},speak(){},keepScreenOn(){},exitApp(){}}` and use `page.clock` to step past the 5 s guard.
   Screenshot at 384×824 and look at it.
3. Commit, push to `main`, then watch the build: `gh run watch <id> -R Tenet71/lap-counter --exit-status`.
   Confirm `gh api repos/Tenet71/lap-counter/releases/latest --jq .tag_name` moved on.
4. Give the owner the install link: https://github.com/Tenet71/lap-counter/releases/latest/download/LapCounter.apk
   Remind them the new download may be named "LapCounter (1).apk", and to install over the old app, never uninstall first.
5. Update the docs to match: README.md, the Word file "Lap Counter - Documentation.docx" (user guide first, then
   technical notes, then version history), and the Notion note "Lap Counter app – documentation"
   (Notes database, linked to the Software project). Add a line to the version history.

Docs-only pushes (`*.md`) don't trigger a build. For any other non-app change, put `[skip ci]` in the commit message
to avoid publishing an identical release.

## Known limitations / backlog

- No export or backup of history. Uninstalling or clearing app data deletes it.
- The keystore and its password are in this public repo. Fine for personal sideloading, but making the repo private would close the gap.
- GitHub warns that some Actions versions are being phased out. They will need bumping eventually.
