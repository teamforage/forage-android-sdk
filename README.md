# Forage Android SDK (`v3.10.0`)

# Forked for POS Terminal Key Management

Forage maintains an additional layer of PIN security for POS terminals, specifically the use of the DUKPT key rotation protocol. We maintain a private fork of our public Android SDK in order to keep the key management code private.

## Maintaining the POS Terminal fork

### Setting the public SDK as a remote

In your local copy of THIS repository, you will need to set up the public SDK as a remote repo. There will be two remotes: origin (this repo) and upstream (the public SDK repo). Run this command to set the public SDK repo as upstream,

`git remote add upstream https://github.com/teamforage/forage-android-sdk.git`

### Rebasing on the public SDK

If you want to pull changes from the public SDK into your branch, then forageuse the following steps in your local repo,

1. `git fetch upstream`
1. `git rebase upstream/main`

You have now synced the private fork with changes from the public SDK upstream.

### Release process

We are using Github's Releases feature to pass built AAR files to Forage's customers. All you need to do is push a tag to this repository. A tag will automatically create a release. A separate GH action will trigger on the tag push, which builds the AAR and upload it to the release as a release asset.

The commands are,

```
git tag vX.X.X
git push origin vX.X.X
```

See .github/workflows/Release.yaml for more insight into the build process. If you are seeing errors, it might be due to reliance on pre-installed build tools on Github's Ubuntu runners. Consult the runner docs [here](https://github.com/actions/runner-images#available-images).

### Semantic Versioning

We will retain the same versions as the public SDK, in order to reduce the number of versioning schemes.

### Keeping the fork slim

We should endeavor to make rebases in this repository as easy as possible. The following principles should guide our changes here,

1. For any key management functions/utilities, keep them in new files as much as possible. There will be no rebase conflicts on files added in this repository.
2. Use shim functions inside the public SDK code that call out to code in separate files. There should only be 3 places that we need to make updates,
  - An initialization function to the ForageTerminalSDK class. Immediately call another function that is contained in a separate file.
  - A function for field level encryption on the PIN before balance checks. Again, immediately call another function that is contained in a separate file.
  - A function for field level encryption on the PIN before capture attempts

# Forked for POS Terminal Key Management

Forage maintains an additional layer of PIN security for POS terminals, specifically the use of the DUKPT key rotation protocol. We maintain a private fork of our public Android SDK in order to keep the key management code private.

## Maintaining the POS Terminal fork

### Setting the public SDK as a remote 

In your local copy of THIS repository, you will need to set up the public SDK as a remote repo. There will be two remotes: origin (this repo) and upstream (the public SDK repo). Run this command to set the public SDK repo as upstream,

`git remote add upstream https://github.com/teamforage/forage-android-sdk.git`

### Rebasing on the public SDK

If you want to pull changes from the public SDK into your branch, use the following steps in your local repo,

1. `git fetch upstream`
1. `git rebase upstream/main`

You have now synced the private fork with changes from the public SDK upstream.

### Release process

We are using Github's Releases feature to pass built APKs to Forage's customers. All you need to do is push a tag to this repository. A tag will automatically create a release. A separate GH action will trigger on the tag push, which builds the APK and upload it to the release as a release asset.

# Forked for POS Terminal Key Management

Forage maintains an additional layer of PIN security for POS terminals, specifically the use of the DUKPT key rotation protocol. We maintain a private fork of our public Android SDK in order to keep the key management code private.

## Maintaining the POS Terminal fork

### Setting the public SDK as a remote 

In your local copy of THIS repository, you will need to set up the public SDK as a remote repo. There will be two remotes: origin (this repo) and upstream (the public SDK repo). Run this command to set the public SDK repo as upstream,

`git remote add upstream https://github.com/teamforage/forage-android-sdk.git`

### Rebasing on the public SDK

If you want to pull changes from the public SDK into your branch, use the following steps in your local repo,

1. `git fetch upstream`
1. `git rebase upstream/main`

You have now synced the private fork with changes from the public SDK upstream.

### Release process

We are using Github's Releases feature to pass built APKs to Forage's customers. All you need to do is push a tag to this repository. A tag will automatically create a release. A separate GH action will trigger on the tag push, which builds the APK and upload it to the release as a release asset.

# Forked for POS Terminal Key Management

Forage maintains an additional layer of PIN security for POS terminals, specifically the use of the DUKPT key rotation protocol. We maintain a private fork of our public Android SDK in order to keep the key management code private.

## Maintaining the POS Terminal fork

### Setting the public SDK as a remote 

In your local copy of THIS repository, you will need to set up the public SDK as a remote repo. There will be two remotes: origin (this repo) and upstream (the public SDK repo). Run this command to set the public SDK repo as upstream,

`git remote add upstream https://github.com/teamforage/forage-android-sdk.git`

### Rebasing on the public SDK

If you want to pull changes from the public SDK into your branch, use the following steps in your local repo,

1. `git fetch upstream`
1. `git rebase upstream/main`

You have now synced the private fork with changes from the public SDK upstream.

### Release process

We are using Github's Releases feature to pass built AAR files to Forage's customers. All you need to do is push a tag to this repository. A tag will automatically create a release. A separate GH action will trigger on the tag push, which builds the AAR and upload it to the release as a release asset.

The commands are,

```
git tag vX.X.X
git push origin vX.X.X
```

See .github/workflows/Release.yaml for more insight into the build process. If you are seeing errors, it might be due to reliance on pre-installed build tools on Github's Ubuntu runners. Consult the runner docs [here](https://github.com/actions/runner-images#available-images).

### Semantic Versioning

We will retain the same versions as the public SDK, in order to reduce the number of versioning schemes.

### Keeping the fork slim

We should endeavor to make rebases in this repository as easy as possible. The following principles should guide our changes here,

1. For any key management functions/utilities, keep them in new files as much as possible. There will be no rebase conflicts on files added in this repository.
2. Use shim functions inside the public SDK code that call out to code in separate files. There should only be 3 places that we need to make updates,
  - An initialization function to the ForageTerminalSDK class. Immediately call another function that is contained in a separate file.
  - A function for field level encryption on the PIN before balance checks. Again, immediately call another function that is contained in a separate file.
  - A function for field level encryption on the PIN before capture attempts

# Forked for POS Terminal Key Management

Forage maintains an additional layer of PIN security for POS terminals, specifically the use of the DUKPT key rotation protocol. We maintain a private fork of our public Android SDK in order to keep the key management code private.

## Maintaining the POS Terminal fork

### Setting the public SDK as a remote 

In your local copy of THIS repository, you will need to set up the public SDK as a remote repo. There will be two remotes: origin (this repo) and upstream (the public SDK repo). Run this command to set the public SDK repo as upstream,

`git remote add upstream https://github.com/teamforage/forage-android-sdk.git`

### Rebasing on the public SDK

If you want to pull changes from the public SDK into your branch, use the following steps in your local repo,

1. `git fetch upstream`
1. `git rebase upstream/main`

You have now synced the private fork with changes from the public SDK upstream.

### Release process

We are using Github's Releases feature to pass built APKs to Forage's customers. All you need to do is push a tag to this repository. A tag will automatically create a release. A separate GH action will trigger on the tag push, which builds the APK and upload it to the release as a release asset.

# Forked for POS Terminal Key Management

Forage maintains an additional layer of PIN security for POS terminals, specifically the use of the DUKPT key rotation protocol. We maintain a private fork of our public Android SDK in order to keep the key management code private.

## Maintaining the POS Terminal fork

### Setting the public SDK as a remote

In your local copy of THIS repository, you will need to set up the public SDK as a remote repo. There will be two remotes: origin (this repo) and upstream (the public SDK repo). Run this command to set the public SDK repo as upstream,

`git remote add upstream https://github.com/teamforage/forage-android-sdk.git`

### Rebasing on the public SDK

If you want to pull changes from the public SDK into your branch, use the following steps in your local repo,

1. `git fetch upstream`
1. `git rebase upstream/main`

You have now synced the private fork with changes from the public SDK upstream.

### Release process

We are using Github's Releases feature to pass built AAR files to Forage's customers. All you need to do is push a tag to this repository. A tag will automatically create a release. A separate GH action will trigger on the tag push, which builds the AAR and upload it to the release as a release asset.

The commands are,

```
git tag vX.X.X
git push origin vX.X.X
```

See .github/workflows/Release.yaml for more insight into the build process. If you are seeing errors, it might be due to reliance on pre-installed build tools on Github's Ubuntu runners. Consult the runner docs [here](https://github.com/actions/runner-images#available-images).

### Semantic Versioning

We will retain the same versions as the public SDK, in order to reduce the number of versioning schemes.

### Keeping the fork slim

We should endeavor to make rebases in this repository as easy as possible. The following principles should guide our changes here,

1. For any key management functions/utilities, keep them in new files as much as possible. There will be no rebase conflicts on files added in this repository.
2. Use shim functions inside the public SDK code that call out to code in separate files. There should only be 3 places that we need to make updates,
  - An initialization function to the ForageTerminalSDK class. Immediately call another function that is contained in a separate file.
  - A function for field level encryption on the PIN before balance checks. Again, immediately call another function that is contained in a separate file.
  - A function for field level encryption on the PIN before capture attempts

# Forked for POS Terminal Key Management

Forage maintains an additional layer of PIN security for POS terminals, specifically the use of the DUKPT key rotation protocol. We maintain a private fork of our public Android SDK in order to keep the key management code private.

## Maintaining the POS Terminal fork

### Setting the public SDK as a remote 

In your local copy of THIS repository, you will need to set up the public SDK as a remote repo. There will be two remotes: origin (this repo) and upstream (the public SDK repo). Run this command to set the public SDK repo as upstream,

`git remote add upstream https://github.com/teamforage/forage-android-sdk.git`

### Rebasing on the public SDK

If you want to pull changes from the public SDK into your branch, use the following steps in your local repo,

1. `git fetch upstream`
1. `git rebase upstream/main`

You have now synced the private fork with changes from the public SDK upstream.

### Release process

We are using Github's Releases feature to pass built APKs to Forage's customers. All you need to do is push a tag to this repository. A tag will automatically create a release. A separate GH action will trigger on the tag push, which builds the APK and upload it to the release as a release asset.

# Forked for POS Terminal Key Management

Forage maintains an additional layer of PIN security for POS terminals, specifically the use of the DUKPT key rotation protocol. We maintain a private fork of our public Android SDK in order to keep the key management code private.

## Maintaining the POS Terminal fork

### Setting the public SDK as a remote 

In your local copy of THIS repository, you will need to set up the public SDK as a remote repo. There will be two remotes: origin (this repo) and upstream (the public SDK repo). Run this command to set the public SDK repo as upstream,

`git remote add upstream https://github.com/teamforage/forage-android-sdk.git`

### Rebasing on the public SDK

If you want to pull changes from the public SDK into your branch, use the following steps in your local repo,

1. `git fetch upstream`
1. `git rebase upstream/main`

You have now synced the private fork with changes from the public SDK upstream.

### Release process

We are using Github's Releases feature to pass built APKs to Forage's customers. All you need to do is push a tag to this repository. A tag will automatically create a release. A separate GH action will trigger on the tag push, which builds the APK and upload it to the release as a release asset.

# Forked for POS Terminal Key Management

Forage maintains an additional layer of PIN security for POS terminals, specifically the use of the DUKPT key rotation protocol. We maintain a private fork of our public Android SDK in order to keep the key management code private.

## Maintaining the POS Terminal fork

### Setting the public SDK as a remote 

In your local copy of THIS repository, you will need to set up the public SDK as a remote repo. There will be two remotes: origin (this repo) and upstream (the public SDK repo). Run this command to set the public SDK repo as upstream,

`git remote add upstream https://github.com/teamforage/forage-android-sdk.git`

### Rebasing on the public SDK

If you want to pull changes from the public SDK into your branch, use the following steps in your local repo,

1. `git fetch upstream`
1. `git rebase upstream/main`

You have now synced the private fork with changes from the public SDK upstream.

### Release process

We are using Github's Releases feature to pass built AAR files to Forage's customers. All you need to do is push a tag to this repository. A tag will automatically create a release. A separate GH action will trigger on the tag push, which builds the AAR and upload it to the release as a release asset.

The commands are,

```
git tag vX.X.X
git push origin vX.X.X
```

See .github/workflows/Release.yaml for more insight into the build process. If you are seeing errors, it might be due to reliance on pre-installed build tools on Github's Ubuntu runners. Consult the runner docs [here](https://github.com/actions/runner-images#available-images).

### Semantic Versioning

We will retain the same versions as the public SDK, in order to reduce the number of versioning schemes.

### Keeping the fork slim

We should endeavor to make rebases in this repository as easy as possible. The following principles should guide our changes here,

1. For any key management functions/utilities, keep them in new files as much as possible. There will be no rebase conflicts on files added in this repository.
2. Use shim functions inside the public SDK code that call out to code in separate files. There should only be 3 places that we need to make updates,
  - An initialization function to the ForageTerminalSDK class. Immediately call another function that is contained in a separate file.
  - A function for field level encryption on the PIN before balance checks. Again, immediately call another function that is contained in a separate file.
  - A function for field level encryption on the PIN before capture attempts

# Forked for POS Terminal Key Management

Forage maintains an additional layer of PIN security for POS terminals, specifically the use of the DUKPT key rotation protocol. We maintain a private fork of our public Android SDK in order to keep the key management code private.

## Maintaining the POS Terminal fork

### Setting the public SDK as a remote 

In your local copy of THIS repository, you will need to set up the public SDK as a remote repo. There will be two remotes: origin (this repo) and upstream (the public SDK repo). Run this command to set the public SDK repo as upstream,

`git remote add upstream https://github.com/teamforage/forage-android-sdk.git`

### Rebasing on the public SDK

If you want to pull changes from the public SDK into your branch, use the following steps in your local repo,

1. `git fetch upstream`
1. `git rebase upstream/main`

You have now synced the private fork with changes from the public SDK upstream.

### Release process

We are using Github's Releases feature to pass built APKs to Forage's customers. All you need to do is push a tag to this repository. A tag will automatically create a release. A separate GH action will trigger on the tag push, which builds the APK and upload it to the release as a release asset.

## SDK reference documentation 
- Refer to the [Forage Terminal Quickstart guide](https://docs.joinforage.app/docs/forage-terminal-android) for the fastest introduction to the SDK

### Run the reference docs locally 
The commands are: 
``
./gradlew dokkaHtml
npx http-serve reference-docs
``

## Run the Sample App

The `/sample-app/` folder in this repository contains a very simple integration of the Forage SDK. To get it running:

1. [Download Android Studio](https://developer.android.com/studio).
  - The app was developed with Android Studio Giraffe and Iguana.
2. Clone this repo to your local machine.
3. In Android Studio, open the cloned `forage-android-sdk` project folder.
  - Android Studio will start downloading the Gradle dependencies. Wait for dependencies to download before moving forward.
4. [Create a session token](https://docs.joinforage.app/reference/create-session-token).
  - **Warning**: While you can [create an authentication token](https://docs.joinforage.app/reference/create-authentication-token) during development instead, **in production client-side requests must use session tokens**.
5. Confirm your Forage Merchant ID in the dashboard ([sandbox](https://dashboard.sandbox.joinforage.app/login/) | [prod](https://dashboard.joinforage.app/login/)).
6. Save your authentication token and Merchant ID as constants in the `TokensViewModel.kt` file.
  - The app will prompt you for an authentication token and Merchant ID on the first page of the app, but it takes defaults from this file.
7. Run the sample app on your emulated device of choice.
8. Use any 16 to 19 digit card number starting with ["9999"](https://docs.joinforage.app/docs/test-ebt-cards#valid-ebt-test-card-numbers) to complete the payment flow.
  - Invalid cards will still be accepted by the Forage Sandbox API.
  - Trigger error scenarios with [these sample cards](https://docs.joinforage.app/docs/test-ebt-cards#invalid-ebt-test-card-numbers).


## Dependencies

- Minimum API Level Android 5.0 (API level 21)
- [kotlinx.coroutines](https://github.com/Kotlin/kotlinx.coroutines) v1.6.4
- 3rd party libraries:
  - [VGS-Collect-Android](https://github.com/verygoodsecurity/vgs-collect-android) v1.7.3
  - [Basis-Theory-Android](https://github.com/Basis-Theory/basistheory-android) v2.5.0
  - [OkHttp](https://github.com/square/okhttp) v4.10.0
  - [Launch Darkly](https://github.com/launchdarkly/android-client-sdk) v4.2.1