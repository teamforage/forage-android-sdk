# Forage Android SDK (`v3.8.1`)

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

## Table of contents

<!--ts-->

- [Overview](#overview)
- [Installation](#installation)
- [Forage Elements: UI Components](#forage-elements-ui-components)
  - [`ForagePANEditText`](#foragepanedittext)
  - [`ForagePINEditText`](#foragepinedittext)
  - [How to style Forage Elements](#how-to-style-forage-elements)
    - [Styling options](#styling-options)
- [Configure Forage](#configure-forage)
- [Create a Forage instance](#create-a-forage-instance)
- [Payment operations](#payment-operations)
  - [Tokenize a card](#tokenize-a-card)
    - [`ForageSDK()`](#foragesdk-1)
    - [`ForageTerminalSDK()`](#forageterminalsdk-1)
  - [Check a card's balance](#check-a-cards-balance)
  - [Collect a card PIN for a payment and defer the capture of the payment to the server](#collect-a-customers-card-pin-for-a-payment-and-defer-the-capture-of-the-payment-to-the-server)
  - [Capture a payment immediately](#capture-a-payment-immediately)
  - [Refund a payment](#refund-a-payment-pos-terminal-only) **(POS-only)**
  - [Collect a card PIN for a refund and defer the completion of the refund to the server](#collect-a-customers-card-pin-for-a-refund-and-defer-the-completion-of-the-refund-to-the-server) **(POS-only)**
- [The ForageApiResponse sealed class](#the-forageapiresponse-sealed-class)
- [Running the sample app](#running-the-sample-app)
  - [Dependencies](#dependencies)
  <!--te-->

## Overview

This documentation explains how to integrate the Forage Android SDK to process online and/or Terminal POS payments.

The SDK provides UI components known as Forage Elements and associated methods that perform payment operations.

The [`ForagePANEditText`](#foragepanedittext) Element collects a customer’s card number. You need a `ForagePANEditText` instance to call the SDK method to [tokenize the card](#tokenize-a-card).

The [`ForagePINEditText`](#foragepinedittext) Element collects a customer’s card PIN. You need a `ForagePINEditText` instance to call the SDK methods that:

- [Check the card's balance](#check-a-cards-balance)
- [Collect a card PIN for a payment and defer the capture of the payment to the server](#collect-a-customers-card-pin-for-a-payment-and-defer-the-capture-of-the-payment-to-the-server)
- [Capture a payment immediately](#capture-a-payment-immediately)
- [Refund a payment](#refund-a-payment-pos-terminal-only) **(POS-only)**

### Step-by-step guides

- [Forage Android SDK Quickstart (online-only)](https://docs.joinforage.app/docs/forage-android-quickstart)
- [Forage Terminal POS Android SDK Quickstart](https://docs.joinforage.app/docs/forage-terminal-android)

## Installation

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

## Forage Elements: UI Components

A `ForageElement` is a secure, client-side entity that accepts and submits customer input for a transaction. These UI components adhere to the [`ForageElement` interface](https://github.com/teamforage/forage-android-sdk/blob/6af970d657095e80ea5ce07f98b12ba031d6e649/forage-android/src/main/java/com/joinforage/forage/android/ui/ForageElement.kt#L15).

The Android SDK includes:

- `ForagePANEditText`: an Element for collecting an EBT Card Number
- `ForagePANEditText`: an Element to collect an EBT Card PIN

To use an Element, add it to a file in your app’s `/layout/` directory, as in the below examples.

### `ForagePANEditText`

A UI text field component for a customer to enter their card number. A `ForagePANEditText` Element is used to [tokenize a card number](#tokenize-a-card).

```xml
<?xml version="1.0" encoding="utf-8"?>
<androidx.constraintlayout.widget.ConstraintLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="match_parent">

    <com.joinforage.forage.android.ui.ForagePANEditText
            android:id="@+id/foragePanEditText"
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:layout_margin="16dp"
            app:layout_constraintBottom_toBottomOf="parent"
            app:layout_constraintEnd_toEndOf="parent"
            app:layout_constraintStart_toStartOf="parent"
            app:layout_constraintTop_toTopOf="parent"
    />

</androidx.constraintlayout.widget.ConstraintLayout>
```

### `ForagePINEditText`

A UI component for a customer to enter their payment method PIN. A `ForagePINEditText` is used to [check the balance of the payment method](#check-a-cards-balance), [defer the capture of a payment to the server](#collect-a-customers-card-pin-for-a-payment-and-defer-the-capture-of-the-payment-to-the-server), or to [capture a payment immediately](#capture-a-payment-immediately).

```xml
<?xml version="1.0" encoding="utf-8"?>
<androidx.constraintlayout.widget.ConstraintLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="match_parent">

    <com.joinforage.forage.android.ui.ForagePINEditText
        android:id="@+id/foragePinEditText"
        android:layout_width="0dp"
        android:layout_height="wrap_content"
        android:layout_margin="16dp"
        app:layout_constraintBottom_toBottomOf="parent"
        app:layout_constraintEnd_toEndOf="parent"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintTop_toTopOf="parent"
    />

</androidx.constraintlayout.widget.ConstraintLayout>
```

### How to style Forage Elements

Whether you’re styling a `ForagePINEditText` or a `ForagePANEditText` Element, the steps are the same. However, keep in mind that the available [customizable properties](#styling-options) differ depending on the Element type.

After you've added an Element component file in your app’s `/layout/` directory, add a theme attribute for the Element to your `attrs.xml`. This example adds an attribute for the PAN Element called `tokenizeForagePANEditTextStyle`:

```xml
<!-- attrs.xml -->

<resources>
    ...
    <!-- Theme attribute for the ForagePANEditText on the tokenize fragment. -->
    <attr name="tokenizeForagePANEditTextStyle" format="reference" />
    ...
</resources>
```

Then apply the attribute as a style tag in the Element component file, as in this example snippet:

```xml
<!-- forage_pan_component.xml -->

<!-- abridged snippet, style tag only -->
<com.joinforage.forage.android.ui.ForagePANEditText
    style="?attr/tokenizeForagePANEditTextStyle"
/>
```

Next, add an `<item>` tag for the style in `themes.xml`, as in the following example:

```xml
<!-- themes.xml -->

<resources>
    <!-- base application theme -->
    <style name="Theme.Forageandroid">
        ...
        <!-- The ForagePANEditText style -->
        <item name="tokenizeForagePANEditTextStyle">@style/TokenizeForagePANEditTextStyle</item>
        ...
    </style>
</resources>
```

Finally, define the style’s properties in `styles.xml`. The below snippet specifies `boxStrokeWidth` and `boxStrokeColor` for the Element:

```xml
<!-- styles.xml -->

<resources>
    ...
    <!-- Style for the ForagePANEditText -->
    <style name="TokenizeForagePANEditTextStyle">
        <item name="panBoxStrokeColor">@color/pan_box_stroke_color</item>
        <item name="panBoxStrokeWidth">1dp</item>
    </style>
    ...
</resources>
```

#### Styling options

`ForagePANEditText` and `ForagePINEditText` have different customizable properties.

Consult the below tables for a comprehensive list of all styling options per Element.

##### `ForagePANEditText` customizable attributes

###### Box styles

| Parameter                  | Part of View |
| -------------------------- | ------------ |
| `panBoxStrokeColor`        | Color        |
| `cornerRadius`             | Dimension    |
| `boxCornerTopEnd`          | Dimension    |
| `boxCornerTopStart`        | Dimension    |
| `boxCornerBottomStart`     | Dimension    |
| `boxCornerBottomEnd`       | Dimension    |
| `panBoxStrokeWidth`        | Dimension    |
| `panBoxStrokeWidthFocused` | Dimension    |

###### Text styles

| Parameter              | Part of View |
| ---------------------- | ------------ |
| `android:textColor`    | Color        |
| `android:textSize`     | Dimension    |
| `textInputLayoutStyle` | Reference    |

##### `ForagePINEditText` customizable attributes

###### Box styles

| Parameter                    | Part of View |
| ---------------------------- | ------------ |
| `boxBackgroundColor`         | Color        |
| `pinBoxStrokeColor`          | Color        |
| `boxCornerRadiusTopEnd`      | Dimension    |
| `boxCornerRadiusTopStart`    | Dimension    |
| `boxCornerRadiusBottomStart` | Dimension    |
| `boxCornerRadiusBottomEnd`   | Dimension    |
| `boxCornerRadius`            | Dimension    |

###### Element styles

| Parameter       | Part of View |
| --------------- | ------------ |
| `elementHeight` | Dimension    |
| `elementWidth`  | Dimension    |

###### Text styles

| Parameter             | Part of View |
| --------------------- | ------------ |
| `hintTextColor`       | Color        |
| `textColor`           | Color        |
| `textSize`            | Dimension    |
| `inputHeight`         | Dimension    |
| `inputWidth`          | Dimension    |
| `pinInputLayoutStyle` | Reference    |
| `hint`                | String       |

## Configure Forage

⚠️ **In order for a `ForageElement` to work properly, you MUST call `setForageConfig()` (Online-only) or `setForagePosConfig()` (Terminal) before calling any other methods.**

The call is the same for `ForagePINEditText` and `ForagePANEditText`.

### `setForageConfig()` - Online-only

The following example calls `setForageConfig` on a `ForagePANEditText` Element:

```kotlin
val onlineOnlyForagePanEditText = root?.findViewById<ForagePANEditText>(
    R.id.tokenizeForagePanEditText
)
onlineOnlyForagePanEditText.setForageConfig(
    ForageConfig(
        merchantId = "mid/<merchant_id>",
        sessionToken = "<session_token>"
    )
)
```

#### `setForageConfig()` parameters

- `ForageConfig`: An object that specifies a `merchantId` and a `sessionToken`.
  - `merchantId`: Either a unique seven digit numeric string that [FNS](https://docs.joinforage.app/docs/ebt-online-101#food-and-nutrition-service-fns) issues to authorized EBT merchants, or a unique merchant ID that Forage provides during onboarding.
  - `sessionToken`: A short-lived token that authenticates front-end requests to Forage. To create one, send a server-side request from your backend to the [`/session_token/`](https://docs.joinforage.app/reference/create-session-token) endpoint.

### `setPosForageConfig()` - Terminal

The following example calls `setPosForageConfig` on a `ForagePINEditText` Element:

```kotlin
val posForagePinEditText = root?.findViewById<ForagePINEditText>(R.id.foragePinEditText)
posForagePinEditText.setPosForageConfig(
    PosForageConfig(
        sessionToken = "<session_token>",
        merchantId = "mid/<merchant_id>"
    )
)
```

#### `setPosForageConfig` parameters

- `PosForageConfig`: An object that specifies a `merchantId` and a `sessionToken`.
    - `merchantId`: Either a unique seven digit numeric string that [FNS](https://docs.joinforage.app/docs/ebt-online-101#food-and-nutrition-service-fns) issues to authorized EBT merchants, or a unique merchant ID that Forage provides during onboarding.
    - `sessionToken`: A short-lived token that authenticates front-end requests to Forage. To create one, send a server-side request from your backend to the [`/session_token/`](https://docs.joinforage.app/reference/create-session-token) endpoint.

## Create a Forage instance

You can opt to create either an online-only or a POS Terminal Forage instance.

### `ForageSDK()`

`ForageSDK()` is the entry point to the Forage SDK. A `ForageSDK` instance interacts with the Forage API.

To create a `ForageSDK` instance, call the constructor, as in the below snippet:

```kotlin
val forage = ForageSDK()
```

You can then use the instance to call methods that perform payment operations. Refer to the [payment operations](#payment-operations) section for documentation on available methods.

### `ForageTerminalSDK()`

`ForageTerminalSDK()` is the entry point to the Forage Terminal SDK. A `ForageTerminalSDK` instance interacts with the Forage API.

To create a `ForageTerminalSDK` instance, call the constructor, passing the `posTerminalId` as the only parameter, as in the following snippet:

```kotlin
val forage = ForageTerminalSDK(posTerminalId)
```

#### `ForageTerminalSDK()` parameters

- `posTerminalId` (_required_): A string that uniquely identifies the POS Terminal used for a transaction.

You can then use the instance to call methods that perform payment operations, as demonstrated in the following section.

#### Initialize a `ForageTerminalSDK` instance
⚠️ **Before you can execute `ForageTerminalSDK` methods, you must call the `init` method on the `ForageTerminalSDK` instance.**

`init` returns the original instance.

```kotlin
val forage = ForageTerminalSDK(posTerminalId)

forage.init(sessionToken)
```

##### `init` parameters
- `sessionToken`: A short-lived token that authenticates front-end requests to Forage. To create one, send a server-side request from your backend to the [`/session_token/`](https://docs.joinforage.app/reference/create-session-token) endpoint.

⚠️ **`init` can take up to 10 seconds to run.** While it doesn’t often take that much time, account for the potential delay in your app’s UX design.

## Payment operations

### Tokenize a card

#### `ForageSDK`

##### `tokenizeEBTCard(TokenizeEBTCardParams)`

⚠️ **This method is online-only. It is only available to `ForageSDK()`.**

This method tokenizes an EBT Card number via a [`ForagePANEditText`](#foragepanedittext) Element.

On success, the object includes a `ref` token that represents an instance of a Forage [`PaymentMethod`](https://docs.joinforage.app/reference/payment-methods#paymentmethod-object). You can store the token for future transactions, like to [check a card's balance](#check-a-cards-balance) or to [create a `Payment`](https://docs.joinforage.app/reference/create-a-payment) in Forage's database.

On failure, for example in the case of [`unsupported_bin`](https://docs.joinforage.app/reference/errors#unsupported_bin), the response includes a list of `ForageError` objects. You can unpack the list to programmatically handle the error and display the appropriate customer-facing message based on the `ForageError.code`.

```kotlin
data class TokenizeEBTCardParams(
    val foragePanEditText: ForagePANEditText,
    val customerId: String?,
    val reusable: Boolean = true
)

suspend fun tokenizeEBTCard(
    params: TokenizeEBTCardParams
): ForageApiResponse<String>
```

###### `TokenizeEBTCardParams`

- `foragePANEditText` (_required_): A reference to the the `ForagePANEditText` Element that you added to your view. This is needed to extract the card number text.
- `customerId`: A unique ID for the customer making the payment. If you use your internal customer ID, then we recommend that you hash the value before sending it on the payload.
- `reusable`: An optional boolean value indicating whether the same card can be used to create multiple payments, set to `true` by default.

###### Example `tokenizeEBTCard()` request

```kotlin
// TokenizeViewModel.kt

class TokenizeViewModel : ViewModel() {
    val merchantId = "mid/<merchant_id>"
    val sessionToken = "<session_token>"

    fun tokenizeEBTCard(foragePanEditText: ForagePANEditText) = viewModelScope.launch {
        val response = ForageSDK().tokenizeEBTCard(
            TokenizeEBTCardParams(
                foragePanEditText = foragePanEditText,
                reusable = true,
                customerId = "<hash_of_customer_id>"
            )
        )

        when (response) {
            is ForageApiResponse.Success -> {
                // parse response.data for the PaymentMethod object
            }
            is ForageApiResponse.Failure -> {
                // do something with error text (i.e. response.message)
            }
        }
    }
}
```

#### `ForageTerminalSDK`

##### Option 1: Tokenize a card via a Forage Element

##### `tokenizeCard(foragePanEditText,reusable)`

⚠️ **This method is POS-only. It is only available to `ForageTerminalSDK()`.**

This method tokenizes a card number via a [`ForagePANEditText`](#foragepanedittext) Element.

On success, the object includes a `ref` token that represents an instance of a Forage [`PaymentMethod`](https://docs.joinforage.app/reference/payment-methods#paymentmethod-object). You can store the token for future transactions, like to [check a card's balance](#check-a-cards-balance) or to [create a `Payment`](https://docs.joinforage.app/reference/create-a-payment) in Forage's database.

On failure, for example in the case of [`unsupported_bin`](https://docs.joinforage.app/reference/errors#unsupported_bin), the response includes a list of `ForageError` objects. You can unpack the list to programmatically handle the error and display the appropriate customer-facing message based on the `ForageError.code`.

###### `tokenizeCard` parameters

- `foragePanEditText` (_required_): A reference to the the `ForagePANEditText` Element that you added to your view. This is needed to extract the card number text.
- `reusable`: An optional boolean value indicating whether the same card can be used to create multiple payments, set to `true` by default.

###### Example `tokenizeCard()` request

```kotlin
// TokenizeViewModel.kt
class TokenizeViewMode : ViewModel() {
    val merchantId = "mid/<merchant_id>"
    val sessionToken = "<session_token>"

    fun tokenizeCard(foragePanEditText: ForagePANEditText) = viewModelScope.launch {

        val response = ForageTerminalSDK().tokenizeCard(
          foragePanEditText = foragePanEditText,
          reusable = true
        )

        when (response) {
            is ForageApiResponse.Success -> {
                // parse response.data for the PaymentMethod object
            }
            is ForageApiResponse.Failure -> {
                // do something with error text (i.e. response.message)
            }
        }
    }
}
```

##### Option 2: Tokenize a card via a magnetic card swipe

##### `tokenizeCard(PosTokenizeCardParams)`

⚠️ **This method is POS-only. It is only available to `ForageTerminalSDK()`.**

To tokenize a card from a magnetic card swipe via a POS Terminal, call `tokenizeCard()` on a `ForageTerminalSDK()` instance, passing `PosTokenizeCardParams`.

On success, the object includes a `ref` token that represents an instance of a Forage [`PaymentMethod`](https://docs.joinforage.app/reference/payment-methods#paymentmethod-object). You can store the token for future transactions, like to [check a card's balance](#check-a-cards-balance) or to [create a `Payment`](https://docs.joinforage.app/reference/create-a-payment) in Forage's database.

On failure, for example in the case of [`unsupported_bin`](https://docs.joinforage.app/reference/errors#unsupported_bin), the response includes a list of `ForageError` objects. You can unpack the list to programmatically handle the error and display the appropriate customer-facing message based on the `ForageError.code`.

###### `PosTokenizeCardParams`

```kotlin
data class PosTokenizeCardParams(
    val forageConfig: PosForageConfig,
    val track2Data: String,
    val reusable: Boolean = true
)
```

- `posForageConfig` (_required_): The configuration details required to authenticate with the Forage API.
  - `merchantId` (_required_): A unique Merchant ID that Forage provides during onboarding onboarding preceded by "mid/". For example, `mid/123ab45c67`. The Merchant ID can be found in the Forage [Sandbox](https://dashboard.sandbox.joinforage.app/login/) or [Production](https://dashboard.joinforage.app/login/) Dashboard.
  - `sessionToken`: A short-lived token that authenticates front-end requests to Forage. To create one, send a server-side request from your backend to the [`/session_token/`](https://docs.joinforage.app/reference/create-session-token) endpoint.
- `track2data` (_required_): The information encoded on Track 2 of the EBT Card’s magnetic stripe, excluding the start and stop sentinels and any LRC characters.
- `reusable`: An optional boolean value indicating whether the same card can be used to create multiple payments, set to `true` by default.

###### Example `tokenizeCard(PosTokenizeCardParams)` request

```kotlin
// TokenizePosViewModel.kt

class TokenizePosViewModel : ViewModel() {
    val merchantId = "mid/<merchant_id>"
    val sessionToken = "<session_token>"

    fun tokenizePosCard(foragePinEditText: ForagePINEditText) = viewModelScope.launch {
        val response = ForageTerminalSDK().tokenizeCard(
          PosTokenizeCardParams(
            forageConfig = ForageConfig(
              merchantId = merchantId,
              sessionToken = sessionToken
            ),
            track2Data = "<read_track_2_data>" // "123456789123456789=123456789123",
          // reusable = true
          )
        )

        when (response) {
            is ForageApiResponse.Success -> {
                // parse response.data for the PaymentMethod object
            }
            is ForageApiResponse.Failure -> {
                // do something with error text (i.e. response.message)
            }
        }
    }
}
```

### Check a card's balance

✅ _The example code in this section uses a `ForageSDK` instance._ If you’re building a Forage Terminal integration, then call methods on a `ForageTerminalSDK` instance instead.

⚠️ **FNS requirements for balance inquiries**

FNS prohibits balance inquiries on sites and apps that offer guest checkout. Skip this section if your customers can opt for guest checkout.
If guest checkout is not an option, then it's up to you whether or not to add a balance inquiry feature. No FNS regulations apply.

#### `checkBalance(CheckBalanceParams)`

This method checks the balance of a previously created [`PaymentMethod`](https://docs.joinforage.app/reference/payment-methods) via a [ForagePINEditText](#foragepinedittext) Element.

On success, the response object includes `snap` and `cash` fields that indicate the EBT Card's current SNAP and EBT Cash balances.

On failure, for example in the case of [`ebt_error_14`](https://docs.joinforage.app/reference/errors#ebt_error_14), the response includes a list of `ForageError` objects. You can unpack the list to programmatically handle the error and display the appropriate customer-facing message based on the `ForageError.code`.

```kotlin
data class CheckBalanceParams(
  val foragePinEditText: ForagePINEditText,
  val paymentMethodRef: String
)

suspend fun checkBalance(
  params: CheckBalanceParams
): ForageApiResponse<String>
```

##### `CheckBalanceParams`

- `foragePinEditText`: A reference to a [`ForagePINEditText`](#foragepinedittext) component.
- `paymentMethodRef`: A unique string identifier for a previously created `PaymentMethod` in Forage's database, found in the response from a call to [`tokenizeEBTCard`](#tokenizeebtcardparams), [`tokenizeCard`](#tokenizecard-parameters),or the [Create a `PaymentMethod` endpoint](https://docs.joinforage.app/reference/create-payment-method).

##### Example `checkBalance()` request

✅ _If you’re using the Forage Terminal SDK, then update `ForageSDK()` in the example below to `ForageTerminalSDK()`._

```kotlin
// BalanceCheckViewModel.kt

class BalanceCheckViewModel : ViewModel() {
    val paymentMethodRef = "020xlaldfh"

    fun checkBalance(foragePinEditText: ForagePINEditText) = viewModelScope.launch {
        val response = ForageSDK().checkBalance(
            CheckBalanceParams(
                foragePinEditText = foragePinEditText,
                paymentMethodRef = paymentMethodRef
            )
        )

        when (response) {
            is ForageApiResponse.Success -> {
                // response.data will have a .snap and a .cash value
            }
            is ForageApiResponse.Failure -> {
                // do something with error text (i.e. response.message)
            }
        }
    }
}
```

### Collect a card PIN for a payment and defer the capture of the payment to the server

✅ _The example code in this section uses a `ForageSDK` instance._ If you’re building a Forage Terminal integration, then call methods on a `ForageTerminalSDK` instance instead.

📘 Check out the [Forage guide to deferred payment capture](https://docs.joinforage.app/docs/capture-ebt-payments-server-side) for step-by-step instructions.

#### `deferPaymentCapture(DeferPaymentCaptureParams)`

This method submits a card PIN via a [`ForagePINEditText`](#foragepinedittext) Element and defers payment capture to the server.

On success, `response.data` resolves with an empty string.

On failure, for example in the case of [`card_not_reusable`](https://docs.joinforage.app/reference/errors#card_not_reusable) or [`ebt_error_51`](https://docs.joinforage.app/reference/errors#ebt_error_51) errors, the response includes a list of `ForageError` objects. You can unpack the list to programmatically handle the error and display the appropriate customer-facing message based on the `ForageError.code`.

```kotlin
data class DeferPaymentCaptureParams(
    val foragePinEditText: ForagePINEditText,
    val paymentRef: String
)

suspend fun deferPaymentCapture(
    params: DeferPaymentCaptureParams
): ForageApiResponse<String>
```

##### `DeferPaymentCaptureParams`

- `foragePinEditText`: A reference to a [`ForagePINEditText`](#foragepinedittext) component.
- `paymentRef`: A unique string identifier for a previously created `Payment` in Forage's database, returned by the [Create a `Payment`](https://docs.joinforage.app/reference/create-a-payment) endpoint.

##### Example `deferPaymentCapture()` request

✅ _If you’re using the Forage Terminal SDK, then update `ForageSDK()` in the example below to `ForageTerminalSDK()`._

```kotlin
class DeferPaymentCaptureViewModel  : ViewModel() {
    val snapPaymentRef = "s0alzle0fal"
    val cashPaymentRef = "d0alsdkfla0"
    val merchantId = "mid/<merchant_id>"
    val sessionToken = "<session_token>"

    fun deferPaymentCapture(foragePinEditText: ForagePINEditText, paymentRef: String) =
        viewModelScope.launch {
            val response = ForageSDK().deferPaymentCapture(
                DeferPaymentCaptureParams(
                    foragePinEditText = foragePinEditText,
                    paymentRef = paymentRef
                )
            )

            when (response) {
                is ForageApiResponse.Success -> {
                    // there will be no financial affects upon success
                    // you need to capture from the server to formally
                    // capture the payment
                }
                is ForageApiResponse.Failure -> {
                    // handle an error response here
                }
            }
        }
}
```

### Capture a payment immediately

✅ _The example code in this section uses a `ForageSDK` instance._ If you’re building a Forage Terminal integration, then call methods on a `ForageTerminalSDK` instance instead.

#### `capturePayment(CapturePaymentParams)`

This method immediately captures a payment via a [`ForagePINEditText`](#foragepinedittext) Element.

On success, the object confirms the transaction. The response includes a Forage [`Payment`](https://docs.joinforage.app/reference/payments) object.

On failure, for example in the case of [`card_not_reusable`](https://docs.joinforage.app/reference/errors#card_not_reusable) or [`ebt_error_51`](https://docs.joinforage.app/reference/errors#ebt_error_51) errors, the response includes a list of `ForageError` objects. You can unpack the list to programmatically handle the error and display the appropriate customer-facing message based on the `ForageError.code`.

```kotlin
data class CapturePaymentParams(
    val foragePinEditText: ForagePINEditText,
    val paymentRef: String
)

suspend fun capturePayment(
  params: CapturePaymentParams
): ForageApiResponse<String>
```

##### `CapturePaymentParams`

- `foragePinEditText`: A reference to a [`ForagePINEditText`](#foragepinedittext) component.
- `paymentRef`: A unique string identifier for a previously created `Payment` in Forage's database, returned by the [Create a `Payment`](https://docs.joinforage.app/reference/create-a-payment) endpoint.

##### Example `capturePayment()` request

✅ _If you’re using the Forage Terminal SDK, then update `ForageSDK()` in the example below to `ForageTerminalSDK()`._

```kotlin
// PaymentCaptureViewModel.kt

class PaymentCaptureViewModel : ViewModel() {
    val snapPaymentRef = "s0alzle0fal"
    val cashPaymentRef = "d0alsdkfla0"
    val merchantId = "mid/<merchant_id>"
    val sessionToken = "<session_token>"

    fun capturePayment(foragePinEditText: ForagePINEditText, paymentRef: String) =
        viewModelScope.launch {
            val response = ForageSDK().capturePayment(
                CapturePaymentParams(
                    foragePinEditText = foragePinEditText,
                    paymentRef = paymentRef
                )
            )

            when (response) {
                is ForageApiResponse.Success -> {
                    // handle successful capture
                }
                is ForageApiResponse.Failure -> {
                    val error = response.errors[0]

                    // handle Insufficient Funds error
                    if (error.code == "ebt_error_51") {
                        val details = error.details as ForageErrorDetails.EbtError51Details
                        val (snapBalance, cashBalance) = details

                        // do something with balances ...
                    }
                }
            }
        }
}
```

### Refund a payment (POS Terminal-only)

#### `refundPayment(PosRefundPaymentParams)`

⚠️ **`refundPayment` is only available for POS, using the `ForageTerminalSDK`.**

This method refunds a payment via a POS Terminal.

On success, the response includes a Forage [`PaymentRefund`](https://docs.joinforage.app/reference/payment-refunds) object.

On failure, for example in the case of [`ebt_error_61`](https://docs.joinforage.app/reference/errors#ebt_error_61), the response includes a list of `ForageError` objects. You can unpack the list to programmatically handle the error and display the appropriate customer-facing message based on the `ForageError.code`.

```kotlin
data class PosRefundPaymentParams(
    val foragePinEditText: ForagePINEditText,
    val paymentRef: String,
    val amount: Float,
    val reason: String,
    val metadata: Map<String, String>? = null
)

suspend fun refundPayment(
    params: PosRefundPaymentParams
): ForageApiResponse<String>
```

##### `PosRefundPaymentParams`

- `foragePinEditText`: A reference to a [`ForagePINEditText`](#foragepinedittext) component.
- `paymentRef`: A unique string identifier for a previously created `Payment` in Forage's database, returned by the [Create a `Payment`](https://docs.joinforage.app/reference/create-a-payment) endpoint.
- `amount`: A positive decimal number that represents how much of the original payment to refund in USD. Precision to the penny is supported. The minimum amount that can be refunded is `0.01`.
- `reason`: A string that describes why the payment is to be refunded.
- `metadata`: A set of optional, merchant-defined key-value pairs. For example, some merchants attach their credit card processor’s ID for the customer making the refund.

##### Example `refundPayment` request

```kotlin
// PosRefundViewModel.kt

class PosRefundViewModel : ViewModel() {
  var paymentRef: String  = ""
  var amount: Float = 0.0
  var reason: String = ""
  var metadata: HashMap? = null


  fun refundPayment(foragePinEditText: ForagePINEditText) = viewModelScope.launch {
    val forage = ForageTerminalSDK(posTerminalId)
    val refundParams = PosRefundPaymentParams(
      foragePinEditText,
      paymentRef,
      amount,
      reason,
      metadata,
    )
    val response = forage.refundPayment(refundParams)

    when (response) {
      is ForageApiResponse.Success -> {
        // do something with response.data
      }
      is ForageApiResponse.Failure -> {
        // do something with response.errors
      }
    }
  }
}
```

### Collect a card PIN for a refund and defer the completion of the refund to the server
### (POS Terminal-only)

#### `deferPaymentRefund(PosDeferPaymentRefundParams)`

This method submits a card PIN via a [`ForagePINEditText`](#foragepinedittext) Element and defers refund completion to the server.

On success, `response.data` resolves with an empty string.

On failure, the response includes a list of `ForageError` objects. You can unpack the list to programmatically handle the error and display the appropriate customer-facing message based on the `ForageError.code`.

```kotlin
data class PosDeferPaymentRefundParams(
    val foragePinEditText: ForagePINEditText,
    val paymentRef: String
)

suspend fun deferPaymentRefund(
    params: PosDeferPaymentRefundParams
): ForageApiResponse<String>
```

##### `PosDeferPaymentRefundParams`

- `foragePinEditText`: A reference to a [`ForagePINEditText`](#foragepinedittext) component.
- `paymentRef`: A unique string identifier for the previously created `Payment` in Forage's database, returned by the [Create a `Payment`](https://docs.joinforage.app/reference/create-a-payment) endpoint when the payment was first created.

##### Example `deferPaymentRefund()` request

```kotlin
// PosDeferPaymentRefundViewModel.kt

class PosDeferPaymentRefundViewModel : ViewModel() {
  var paymentRef: String  = ""

  fun deferPaymentRefund(foragePinEditText: ForagePINEditText) = viewModelScope.launch {
    val forage = ForageTerminalSDK(posTerminalId)
    val deferPaymentRefundParams = PosDeferPaymentRefundParams(
      foragePinEditText,
      paymentRef
    )
    val response = forage.deferPaymentRefund(deferPaymentRefundParams)

    when (response) {
      is ForageApiResponse.Success -> {
        // do something with response.data
      }
      is ForageApiResponse.Failure -> {
        // do something with response.errors
      }
    }
  }
}
```

## The ForageApiResponse sealed class

The SDK provides suspending methods to interact with the Forage API.
`ForageApiResponse` is a sealed class that could be either a `Success` or a `Failure`

```kotlin
sealed class ForageApiResponse<out T> {
    data class Success<out T>(val data: T) : ForageApiResponse<T>()

    data class Failure(val errors: List<ForageError>) : ForageApiResponse<Nothing>()
}
```

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
