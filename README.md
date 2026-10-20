# Lap Counter

A big-button lap counter for walking laps of a short track, for Android.

- Enter the track length and an optional goal, both in metres.
- Tap **LAP** each time you pass the start. Taps within 30 seconds of the last lap are ignored.
- Each lap buzzes, beeps and (if Voice is on) says the lap number and distance.
- When the goal is reached it buzzes longer and keeps counting.
- Pause, resume, stop (save) or reset (discard, with a confirmation).
- Finished walks are kept in History on the phone.
- The screen stays on during a walk.

## Install

Download **LapCounter.apk** from the latest release on the phone and open it:
https://github.com/Tenet71/lap-counter/releases/latest/download/LapCounter.apk

Android will ask to allow installing apps from the browser the first time. Newer versions install over the old one and keep your history.

## How it is built

Every push to `main` builds a signed APK with GitHub Actions and publishes it as a new release. The app is a full-screen WebView showing `app/src/main/assets/index.html`, with a small native bridge for vibration, text-to-speech and keep-screen-on.
