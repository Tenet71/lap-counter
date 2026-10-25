# Lap Counter

A big-button lap counter for walking laps of a short track, for Android.

- Enter the track length and an optional goal, both in metres.
- Tap **LAP** each time you pass the start. Taps within 5 seconds of the last lap are ignored.
- Each lap buzzes, beeps and (if Voice is on) says the lap number and distance.
- When the goal is reached it buzzes longer and keeps counting.
- The walk screen shows your average speed so far in km/h, updated after each lap.
- The laps and distance numbers are sized to fit up to 999 laps and 9,999 m side by side.
- Pause, resume, stop (save) or reset (discard, with a confirmation).
- Finished walks are kept in History on the phone.
- The screen stays on during a walk.

## Install

Download **LapCounter.apk** from the latest release on the phone and open it:
https://github.com/Tenet71/lap-counter/releases/latest/download/LapCounter.apk

Android will ask to allow installing apps from the browser the first time. Newer versions install over the old one and keep your history.

## How it is built

Every push to `main` builds a signed APK with GitHub Actions and publishes it as a new release. The app is a full-screen WebView showing `app/src/main/assets/index.html`, with a small native bridge for vibration, text-to-speech and keep-screen-on.

## Version history

- **1.0.1:** first release.
- **1.0.2:** ✕ close button on the Home screen.
- **1.0.3:** decimal point on the keypad for track lengths like 23.5 m; hold ⌫ to clear.
- **1.0.4:** repeat taps ignored for 5 s instead of 30 s.
- **1.0.5:** laps and distance no longer overlap (sized for 999 laps and 9,999 m); average speed so far shown on the walk screen.
