# Android Frontend Test Suite

This project includes:
- JVM unit tests for the app module using Robolectric.
- Instrumentation tests for UI flows using Espresso.
- JVM unit tests for the utilities module.

How to run:
- JVM unit tests (app + utilities):
  ./gradlew :app:testDebugUnitTest :utilities:testDebugUnitTest
  # Or run all unit tests (no device required)
  ./gradlew test

- All unit tests:
  ./gradlew test

- Instrumentation tests (require emulator or device):
  ./gradlew :app:connectedDebugAndroidTest

Notes:
- Network calls in MainActivity use HttpURLConnection. Robolectric tests avoid real network by testing parse/bind logic and input validation.
- Default API key in strings.xml is a placeholder; missing key path is verified by tests.
