# Tack Tracker

Android app that tells a blind helmsperson, by sound, which tack the boat is on in match racing.
Built from the specification, one milestone at a time. See `DECISIONS.md` for assumptions.

**Status:** milestone 2 of 8 (sensors, heading fusion, debug screen).

## Build
1. Open in Android Studio (creates the Gradle wrapper), or run `gradle wrapper` once.
2. `./gradlew assembleDebug`
3. `./gradlew test`

**Safety:** this app is an aid only. It does not replace the rules of racing, a crew's lookout or common sense.
