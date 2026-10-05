# DECISIONS

Assumptions and design decisions, newest at the bottom of each milestone.

## Milestone 1
- **Package / applicationId:** `com.blindsail.tacktracker`. Change in `app/build.gradle.kts` and the folder structure if you want a different one.
- **UI toolkit:** Jetpack Compose (Material 3), per spec "Compose or View". Compose has good TalkBack support.
- **Versions:** AGP 8.7.3, Kotlin 2.0.21, compile/target SDK 35 (spec says "latest stable"; bump when needed), JDK 17 target.
- **Gradle wrapper not included.** Generate it once with `gradle wrapper` or open the project in Android Studio, which creates it. Then `./gradlew assembleDebug` and `./gradlew test` work as required.
- **Settings is Android-free.** `Settings` is a plain data class in `settings/`; the DataStore persistence layer is added in milestone 7 and will live in a separate file.
- **0 means "off"** for `repeatSeconds` and `heartbeatSeconds`; `coerced()` clamps all other values to the ranges in spec section 16.
- **Extra settings** not in the section 16 table (steady threshold, min tack change, etc.) hold the defaults given in the body text of the spec.
- **Mounting options:** the spec lists "3 options" but names two. Third option added: flat, top to stern.
- **AngleMath edge cases:** `circularMean` returns 0.0 when vectors cancel exactly; `circularStdDev` returns 0.0 for fewer than 2 samples and +Infinity when angles cancel.
- **Test verification:** tests are JUnit 5. They were also compiled and run with a standalone Kotlin compiler and a tiny JUnit shim (all 9 pass); they have not yet been run through Gradle.

## Milestone 2
- **HeadingFusion is pure Kotlin** (no Android imports) but lives in `sensors/` as the spec's package layout says; `CompassSource` and `LocationSource` are the only Android-dependent files there. Shared types (`GpsFix`, `CompassSample`, `HeadingState`) are in `HeadingFusion.kt`. `Confidence` is in `logic/` because later logic reuses it.
- **One monotonic clock.** All timestamps are `SystemClock.elapsedRealtime()` milliseconds (GPS: `Location.elapsedRealtimeNanos`; compass: time of the callback, not `SensorEvent.timestamp`, which is not on the same clock on every device).
- **COG anchored, compass interpolated (deviation from the literal spec text).** The spec says "use COG when valid". GPS gives COG only about once per second, which would make heading move in 1 Hz steps and slow tack detection. So at each valid fix the output equals COG, and between fixes the compass turn since that fix is added. Without a compass it simply holds the last COG.
- **COG validity:** speed >= `gpsMinSpeed`, bearing accuracy <= 20 deg when reported (new `maxBearingAccuracyDeg` in `HeadingFusion.Config`), and fix not older than 3 s. A slow or poor fix switches to compass immediately.
- **Compass offset** initialises to the first measured value (otherwise it would take about 20 s to converge from zero), then follows a circular low-pass with a 20 s time constant, updated only on valid fixes.
- **Confidence:** HIGH = valid COG; MEDIUM = compass with an offset updated within 60 s; LOW = otherwise. No input at all gives a null heading.
- **Smoothing** default 0.75 s time constant (inside the spec's 0.5-1 s), set by `Settings.headingSmoothingSeconds`.
- **Phone mounting:** "flat, top to bow" uses azimuth of the top edge; "upright" assumes the back of the phone points forward (azimuth of the -Z axis); "flat, top to stern" adds 180 deg. The upright option is my interpretation of "portrait upright in a pocket facing forward"; verify on a real phone.
- **Debug screen** (`ui/DebugScreen.kt`): sensors run only while the activity is started; screen values refresh at 4 Hz so a screen reader is not flooded; mounting can be switched there (settings persistence arrives in milestone 7). All buttons are at least 64 dp.
- **Location without Google Play services** is not supported (FusedLocationProviderClient only). A `LocationManager` fallback could be added later.
- **Android code is not compiled here.** Only the pure-Kotlin logic and its 21 tests were compiled and run in my environment. Expect that the first Gradle sync may need small fixes in the Android files.
