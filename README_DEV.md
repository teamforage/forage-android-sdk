# Development
## Android Studio
This project was developed using Android Studio Electric Eel, but you can also use Android Studio Dolphin since it's currently using Android Gradle Plugin version 7.3.1.

## Get up and running locally
1. You need to install [the Java Developer Kit (JDK)](https://www.oracle.com/java/technologies/downloads/). The JDK is what powers Android Studio ability to build and run Android apps. Technically, our CI/CD builds Forage Android SDK using [JDK 11](https://github.com/teamforage/forage-android-sdk/blob/f2ff61ed27847b28d631f975c9a79e52f9258802/.github/workflows/CI.yaml#L21). However, as of this writing, the latest stable version is [JDK 17](https://www.oracle.com/java/technologies/downloads/#java17).
    > ⚠️ NOTE: Installing the Java Developer Kit (JDK) is **not** the same thing as “[installing Java on your machine](https://www.oracle.com/ca-en/java/technologies/downloads/)”. The latter refers to installing the Java Runtime Environment (JRE), which is what you would install to run a Java application. As we are developing Java (Android) applications, we the JDK which includes (is a superset of) the JRE.
2. Once you have installed the JDK of your choice to your local machine, you will need to make Android Studio aware by setting the `JAVA_HOME` environment variable to the path on your machine that the JDK resides. For example:
   ```bash
    # Windows
   JAVA_HOME=C:\Program Files\Java\jdk-17
   ```
3. Confirm that the JDK and Android Studio are working as expected by running the [unit tests below](#How to run the unit tests). You should not see any errors. This may take a minute or two to run.
4. Now it's time to run the Sample App via local emulator. The below summarizes the few steps involved but see the [docs](https://developer.android.com/studio/run/managing-avds) for more details
    
   a. Click the Devices Manager Icon. It should be in the top right corner of Android Studio by default 

   b. Create a **virtual** device. You probably have no virtual devices if you've just installed Android Studio

   c. Now install a System Image that the Virtual Device will run. You can choose "R", for example

   d. Now, in the top right of Android Studio, select the Virtual Device you installed and select `sample-app` as the program to run. Click the play ▶️button to run. 

   e. That's it! After some time, you should see the Sample App running on virtual device emulator. 


## How to run the unit tests
Run the test task for dev debug build variant:
```shell
./gradlew testDevDebugUnitTest  
```

Currently, our build variants are only changing env vars, so any `test<dev/staging/cert/sandbox/prod><debug/release>UnitTest` variants should run the same tests producing the same result.

## Code coverage
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

## Code formatting
This project uses [Spotless](https://github.com/diffplug/spotless) to format the code. Before pushing the code, you may need to run the following:

```shell
 ./gradlew spotlessCheck # Checks that sourcecode satisfies formatting steps 
 ./gradlew spotlessApply  # Applies code formatting steps to sourcecode in-place
```

### Optimizing SVGs
We can run [avocado](https://github.com/alexjlockwood/avocado) command line tool to optimize the SVGs before importing them to the project.
