# Forage Android SDK (`v3.8.0`)

You can use the Forage Android SDK to process online-only and/or Terminal POS EBT payments. The SDK provides UI components known as Forage Elements and associated methods that perform payment operations.

Get started with our Quickstart guides ([online-only](https://docs.joinforage.app/docs/forage-android-quickstart), [POS](https://docs.joinforage.app/docs/forage-terminal-android)) and [reference documentation](https://android.joinforage.app/), or keep reading for instructions on how to [install the library](#install-the-library) or [run the sample app](#run-the-sample-app).

## Install the library

To install the SDK, add this dependency to your module `build.gradle`:

```groovy
plugins {
    id 'com.android.application'
    id 'kotlin-android'
}

android {
    ...
}

dependencies {
    ...

    // Forage Android SDK
    implementation 'com.joinforage:forage-android:<sem_ver>'

    ...
}
```

## Run the Sample App

The sample-app/ folder in this repository contains a very simple integration of the Forage SDK. To get it running:

1. [Download Android Studio](https://developer.android.com/studio).
   i. The app was developed with Android Studio Electric Eel.
2. Clone this repo to your local machine.
3. In Android Studio, open the cloned `forage-android-sdk` project folder
   i. Android Studio will start downloading the Gradle dependencies. Wait for dependencies to download before moving forward.
4. [Create a bearer token](https://docs.joinforage.app/recipes/generate-a-token) with `pinpad_only` scope.
5. Confirm your FNS number on the Forage dashboard ([sandbox](https://dashboard.sandbox.joinforage.app/login/) | [prod](https://dashboard.joinforage.app/login/)).
6. Place your bearer token and FNS number in constants inside `sample-app/java/com.joinforage.android.example/ui/complete.flow/tokens/model/TokensUIDefaultState.kt`.
   i. The sample-app will prompt you for a bearer token and FNS number on the first page of the app, but takes defaults from this file location.
7. Run the sample-app on your emulated device of choice.
8. Use any 16 to 19 digit card number starting with ["9999"](https://docs.joinforage.app/docs/test-ebt-cards#valid-ebt-test-card-numbers) to complete the payment flow.
   i. Invalid cards will still be accepted by the Forage Sandbox API.
   ii. Trigger error scenarios with [these sample cards](https://docs.joinforage.app/docs/test-ebt-cards#invalid-ebt-test-card-numbers).

## Dependencies

- Minimum API Level Android 5.0 (API level 21)
- [kotlinx.coroutines](https://github.com/Kotlin/kotlinx.coroutines) v1.6.4
- 3rd party libraries:
  - [VGS-Collect-Android](https://github.com/verygoodsecurity/vgs-collect-android) v1.7.3
  - [Basis-Theory-Android](https://github.com/Basis-Theory/basistheory-android) v2.5.0
  - [OkHttp](https://github.com/square/okhttp) v4.10.0
  - [Launch Darkly](https://github.com/launchdarkly/android-client-sdk) v4.2.1
