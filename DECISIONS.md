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
