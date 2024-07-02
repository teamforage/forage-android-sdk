package com.joinforage.android.example.ui.pos.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.viewinterop.AndroidView
import com.joinforage.forage.android.pos.ui.element.ForagePINEditText

@Composable
fun ComposableForagePINEditText(
    withPinElementReference: (element: ForagePINEditText) -> Unit
) {
    AndroidView(
        factory = { context ->
            ForagePINEditText(context).apply {
                this.requestFocus()
                setOnChangeEventListener {
                    // since the sample app supports both a keypad and
                    // this PinEditText, we tell the sample app to
                    // assume the user wants to use the EditText if
                    // they change the EditText value. It's possible
                    // that the keypad is considered the current PIN
                    // element so a user need only change the value of
                    // EditText to tell the app to consider (again)
                    // the EditText and not the keypad
                    withPinElementReference(this)
                }
            }
        }
    )
}

@Preview
@Composable
fun ComposableForagePINEditTextPreview() {
    ComposableForagePINEditText(
        withPinElementReference = {}
    )
}
