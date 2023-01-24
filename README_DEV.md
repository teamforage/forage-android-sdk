## Development
### Android Studio
This project was developed using Android Studio Electric Eel, but you can also use Android Studio Dolphin since it's currently using Android Gradle Plugin version 7.3.1.

### How to run the unit tests
Run the test task for dev debug build variant:
```shell
./gradlew testDevDebugUnitTest  
```

Currently, our build variants are only changing env vars, so any `test<dev/staging/cert/sandbox/prod><debug/release>UnitTest` variants should run the same tests producing the same result.

### Code coverage
We use [Kover](https://github.com/Kotlin/kotlinx-kover) to extract our code coverage.
To check the SDK coverage, you can run:

```shell
 ./gradlew forage-android:koverHtmlReport
```

Kover will provide the report link when it finishes running:

```shell
> Task :forage-android:koverHtmlReport
Kover: HTML report for ':forage-android' file:///<project_path>/forage-android/forage-android/build/reports/kover/html/index.html
```
- We are not filtering out classes/files that unit tests will not cover.

### Code formatting
This project uses [Spotless](https://github.com/diffplug/spotless) to format the code. Before pushing the code, you may need to run the following:

```shell
 ./gradlew spotlessCheck # Checks that sourcecode satisfies formatting steps 
 ./gradlew spotlessApply  # Applies code formatting steps to sourcecode in-place
```

### Optimizing SVGs
We can run [avocado](https://github.com/alexjlockwood/avocado) command line tool to optimize the SVGs before importing them to the project.
