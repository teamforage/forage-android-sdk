# forage-android-sdk

Forage Android SDK

# Table of contents

<!--ts-->

- [Overview](#overview)
- [Installation](#installation)
- [UI Components](#ui-components)
  - [ForagePANEditText](#foragepanedittext)
  - [ForagePINEditText](#foragepinedittext)
  - [How to provide styles](/docs/styles.md)
- [Tokenizing an EBT Card](#tokenizing-an-ebt-card)
- [Performing a balance check](#performing-a-balance-check)
- [Capturing a payment](#capturing-a-payment)
- [The ForageApiResponse sealed class](#the-forageapiresponse-sealed-class)
- [Running the Sample App](#running-the-sample-app)
- [Dependencies](#dependencies)
<!--te-->

## Overview

This documentation explains how to integrate the Forage Android SDK to process EBT payments.

In addition to [UI components](#ui-components), the SDK provides APIs for:

1. [Tokenizing an EBT Card](#tokenizing-an-ebt-card)
2. [Performing a balance check](#performing-a-balance-check)
3. [Capturing a payment](#capturing-a-payment)

Read on for installation instructions and details about the APIs.

## Installation

To install the SDK, add this dependency to your module `build.gradle`

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

The Forage Android SDK comes with a flavor dimension `version` with values of `prod` and `sandbox`. Your flavor of choice can be specified in the `defaultConfig` block:

```groovy
android {
    defaultConfig {
        missingDimensionStrategy("version", "sandbox")
    }
}
```

Or, you can specify the flavor in each of your own `productFlavors`:

```groovy
android {
    flavorDimensions "exampleDimension"
    productFlavors {
        "production" {
            missingDimensionStrategy("version", "prod", "sandbox")
        }
        "staging" {
            missingDimensionStrategy("version", "sandbox", "prod")
        }
    }
}
```

More information on using variant-aware dependencies can be found in the [Android developer docs](https://developer.android.com/build/build-variants#variant_aware).

## UI Components

### ForagePANEditText

A UI component for a customer to enter their EBT card number.

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
        app:layout_constraintTop_toTopOf="parent" />

</androidx.constraintlayout.widget.ConstraintLayout>
```

### ForagePINEditText

A UI component for a customer to enter their EBT card PIN.

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
        app:layout_constraintTop_toTopOf="parent" />

</androidx.constraintlayout.widget.ConstraintLayout>
```

# Usage

## Tokenizing an EBT Card

### Step 1: Add the `ForagePANEditText` UI component

First, you need to add `ForagePANEditText` to your layout file:

```xml
<androidx.constraintlayout.widget.ConstraintLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="match_parent">

    <com.joinforage.forage.android.ui.ForagePANEditText
        android:id="@+id/tokenizeForagePanEditText"
        android:layout_width="0dp"
        android:layout_height="wrap_content"
        android:padding="16dp"
        app:layout_constraintBottom_toBottomOf="parent"
        app:layout_constraintEnd_toEndOf="parent"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintTop_toTopOf="parent" />

</androidx.constraintlayout.widget.ConstraintLayout>

```

Since `ForagePANEditText` is currently not receiving any style from your theme, it should look like this:

<img src="screenshots/forage_pan_edit_text_no_style.png" width="300" height="500">

#### Customizing `ForagePANEditText`

To provide a style to your `ForagePANEditText`, you need to include these two themes attributes on your `attrs.xml` file:

```xml
<resources>
    ...
    <!-- Theme attribute for the ForagePANEditText on the tokenize fragment. -->
    <attr name="tokenizeForagePANEditTextStyle" format="reference" />
    <attr name="tokenizeForageTextInputLayoutStyle" format="reference" />
    ...
</resources>
```

Now you can add the style to your `ForagePANEditText`:

```xml
<androidx.constraintlayout.widget.ConstraintLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="match_parent">

    <com.joinforage.forage.android.ui.ForagePANEditText
        android:id="@+id/tokenizeForagePanEditText"
        style="?tokenizeForagePANEditTextStyle"
        android:layout_width="0dp"
        android:layout_height="wrap_content"
        android:padding="16dp"
        app:layout_constraintBottom_toBottomOf="parent"
        app:layout_constraintEnd_toEndOf="parent"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintTop_toTopOf="parent" />

</androidx.constraintlayout.widget.ConstraintLayout>

```

Here is the relevant part from the application theme that shows the styles that are assigned to the `ForagePANEditText`:

```xml
<resources>
    <!-- Base application theme. -->
    <style name="Theme.Forageandroid" parent="Theme.MaterialComponents.DayNight.DarkActionBar">
        ...
        <!-- The ForagePanEditText shown in tokenize Fragment -->
        <item name="tokenizeForagePANEditTextStyle">@style/TokenizeForagePANEditTextStyle</item>
        <item name="tokenizeForageTextInputLayoutStyle">@style/TokenizeForageTextInputLayoutStyle</item>
        ...
    </style>
</resources>
```

Finally, here are the assigned styles:

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    ...
    <style name="DefaultForagePANEditTextStyle">
        <!-- Default properties can be added here that will be applied to every ForagePANEditText. -->
    </style>

    <!-- Style for the ForagePANEditText -->
    <style name="TokenizeForagePANEditTextStyle" parent="DefaultForagePANEditTextStyle">
        <item name="textInputLayoutStyle">@attr/tokenizeForageTextInputLayoutStyle</item>
    </style>

    <style name="TokenizeForageTextInputLayoutStyle" parent="Widget.MaterialComponents.TextInputLayout.OutlinedBox">
        <item name="android:hint">@string/tokenize_forage_edit_text_hint</item>
        <item name="boxStrokeWidth">1dp</item>
    </style>
    ...
</resources>
```

<img src="screenshots/forage_pan_some_examples.png" width="300" height="500">

### Step 2: Tokenize the EBT card number

The ForageSDK exposes the following function to collect the EBT card number:

```kotlin
    suspend fun tokenizeEBTCard(
        merchantAccount: String,
        bearerToken: String,
        customerId: String
    ): ForageApiResponse<String>
```

#### Parameter definitions

- `merchantAccount`: A unique seven digit numeric string that [FNS](https://docs.joinforage.app/docs/ebt-online-101#food-and-nutrition-service-fns) issues to authorized EBT merchants.
- `bearerToken`: A [session token](https://docs.joinforage.app/reference/create-session-token) that authenticates front-end requests to Forage. To create one, send a server-side request from your backend to the `/session_token/` endpoint.
- `customerId`: A unique ID for the end customer making the payment. If you use your internal customer ID, then we recommend that you hash the value before sending it on the payload.

#### Example

This is an example of usage inside an ACC ViewModel:

```kotlin
    fun onSubmit() = viewModelScope.launch {
        _isLoading.value = true

        val response = ForageSDK.tokenizeEBTCard(
            merchantAccount = merchantAccount,
            bearerToken = bearer,
            // NOTE: The following line is for testing purposes only and should not be used in production.
            // Please replace this line with a real hashed customer ID value.
            customerId = UUID.randomUUID().toString()
        )

        when (response) {
            is ForageApiResponse.Success -> {
                val adapter: JsonAdapter<PaymentMethod> = moshi.adapter(PaymentMethod::class.java)

                val result = adapter.fromJson(response.data)

                _paymentMethod.value = result
            }
            is ForageApiResponse.Failure -> {
                _error.value = response.message
            }
        }

        _isLoading.value = false
    }
```

## Performing a balance check

### Step 1: Add the `ForagePINEditText` UI component

You need to add the `ForagePINEditText` component to your app to check a card's balance. If you need help, refer to the instructions for [adding the UI component](#step-1-add-the-foragepanedittext-ui-component) in the method above. Just change the component name!

### Step 2: Check the balance of the EBT Card

The ForageSDK exposes the following function to check the balance of an EBT card:

```kotlin
    suspend fun checkBalance(
        context: Context,
        pinForageEditText: ForagePINEditText,
        merchantAccount: String,
        bearerToken: String,
        paymentMethodRef: String
    ): ForageApiResponse<String>
```

To keep the sensitive information PCI compliant, this function needs the `ForagePINEditText` reference and the `Context` so that Forage can cancel ongoing requests properly. The `paymentMethodRef` parameter is a string identifier that refers to an instance in Forage's database of a [`PaymentMethod`](https://docs.joinforage.app/reference/create-payment-method), a tokenized representation of an EBT Card.

This is an example of usage inside an ACC ViewModel:

```kotlin
    fun checkBalance(context: Context, pinForageEditText: ForagePINEditText) =
    viewModelScope.launch {
        _isLoading.value = true

        val response = ForageSDK.checkBalance(
            context = context,
            pinForageEditText = pinForageEditText,
            merchantAccount = merchantAccount,
            bearerToken = bearer,
            paymentMethodRef = paymentMethodRef
        )

        when (response) {
            is ForageApiResponse.Success -> {
                println(response.data)

                val adapter: JsonAdapter<BalanceResponse> =
                    moshi.adapter(BalanceResponse::class.java)

                val result = adapter.fromJson(response.data)

                if (result != null) {
                    _snap.value = "SNAP: ${result.snap}"
                    _cash.value = "CASH: ${result.cash}"
                    _isLoading.value = false
                    _isNextVisible.value = true
                }
            }
            is ForageApiResponse.Failure -> {
                _isLoading.value = false
                _error.value = response.message
            }
        }
    }
```

### (Optional) Step 3: Persist the PaymentMethod ref in your wallet

If you offer customers a wallet to save their payment methods for future use, then you need to link the EBT PaymentMethod ref to that wallet.

## Capturing a payment

### Step 1: Add the `ForagePINEditText` UI component

You need to add the `ForagePINEditText` component to your app to authorize a payment capture. If you need help, refer to the instructions for [adding the UI component](#step-1-add-the-foragepanedittext-ui-component) in the method above. Just change the component name!

## Step 2: Send a server-side POST to the Forage `/payments/` endpoint to create a `Payment` object

Your backend needs to create the object. You'll need the `ref` from the response for Step 3.

### Step 3: Capture the EBT payment

The ForageSDK exposes the following function to capture an EBT payment:

```kotlin
    suspend fun capturePayment(
        context: Context,
        pinForageEditText: ForagePINEditText,
        merchantAccount: String,
        bearerToken: String,
        paymentRef: String
    ): ForageApiResponse<String>
```

This is an example of usage inside an ACC ViewModel:

```kotlin

    fun captureSnapAmount(context: Context, pinForageEditText: ForagePINEditText) =
        viewModelScope.launch {
            _uiState.value = _uiState.value!!.copy(isLoading = true)

            val response = ForageSDK.capturePayment(
                context = context,
                pinForageEditText = pinForageEditText,
                merchantAccount = merchantAccount,
                bearerToken = bearer,
                paymentRef = snapPaymentRef
            )

            when (response) {
                is ForageApiResponse.Success -> {
                    _uiState.value = _uiState.value!!.copy(
                        isLoading = false,
                        snapResponse = response.data
                    )
                }
                is ForageApiResponse.Failure -> {
                    _uiState.value = _uiState.value!!.copy(
                        isLoading = false,
                        snapResponse = response.message
                    )
                }
            }
        }
```

The `paymentRef` can be used to determine what type of tender is being captured (SNAP or EBT Cash).

#### Capture both SNAP and EBT Cash payments

If you choose to support both SNAP and EBT Cash, then your implementation needs to handle two payments.

You can also make two calls to `ForageSDK.capturePayment` to capture both payments with a single action, and then process the two responses to determine what is shown to the user, as in the following example inside an ACC ViewModel:

```kotlin
    fun captureBothAmounts(
        context: Context,
        cashPinForageEditText: ForagePINEditText,
        snapPinForageEditText: ForagePINEditText
    ) = viewModelScope.launch {
        _uiState.value = _uiState.value!!.copy(isLoading = true)

        val cashResponse = ForageSDK.capturePayment(
            context = context,
            pinForageEditText = cashPinForageEditText,
            merchantAccount = merchantAccount,
            bearerToken = bearer,
            paymentRef = cashPaymentRef
        )

        val snapResponse = ForageSDK.capturePayment(
            context = context,
            pinForageEditText = snapPinForageEditText,
            merchantAccount = merchantAccount,
            bearerToken = bearer,
            paymentRef = snapPaymentRef
        )
    }
```

In this example, both requests are executed sequentially. To run them in parallel, you could use `async`/`await` to launch the `capturePayment` call.

## The ForageApiResponse sealed class

The SDK provides suspending functions to interact with the Forage API.
`ForageApiResponse` is a sealed class that could be either a `Success` or a `Failure`

```kotlin
sealed class ForageApiResponse<out T> {
    data class Success<out T>(val data: T) : ForageApiResponse<T>()

    data class Failure(val errors: List<ForageError>) : ForageApiResponse<Nothing>()
}
```

## Running the Sample App

The sample-app/ folder in this repository contains a very simple integration of the Forage SDK. To get it running,

1. [Download Android Studio](https://developer.android.com/studio)
   1. The app was developed with Android Studio Electric Eel
2. Open the forage-android-sdk project folder and wait for dependencies to download
3. [Create a bearer token](https://docs.joinforage.app/recipes/generate-a-token) with `pinpad_only` scope
4. Confirm your FNS number on the Forage dashboard ([sandbox](https://dashboard.sandbox.joinforage.app/login/) | [prod](https://dashboard.joinforage.app/login/))
5. Place your bearer token and FNS number in constants inside sample-app/java/com.joinforage.android.example/ui/complete.flow/tokens/model/TokensUIDefaultState.kt
   1. The sample-app will prompt you for a bearer token and FNS number on the first page of the app, but takes defaults from this file location
6. Choose the appropriate build variant (usually sandboxDebug for your first run)
7. Run the sample-app on your emulated device of choice
8. Use any 16 digit card number to complete the payment flow
   1. Invalid cards will still be accepted by the Forage Sandbox API
   2. Trigger error scenarios with [these sample cards](https://docs.joinforage.app/docs/how-to-trigger-exceptions)

## Dependencies

- Minimum API Level Android 5.0 (API level 21)
- [kotlinx.coroutines](https://github.com/Kotlin/kotlinx.coroutines) v1.6.4
- 3rd party libraries:
  - [VGS-Collect-Android](https://github.com/verygoodsecurity/vgs-collect-android) v1.7.3
    - [OkHttp](https://github.com/square/okhttp) v4.10.0
