package com.joinforage.android.example.ui.pos.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidViewBinding
import com.joinforage.android.example.databinding.ForagePinPadExampleBinding
import com.joinforage.forage.android.pos.ui.element.ForagePinPad

@Composable
fun ComposableForagePinPad(
    withPinElementReference: (element: ForagePinPad) -> Unit
) {
    val text = remember { mutableStateOf("") }
    val renderedText by text
    Text(
        text = renderedText,
        fontSize = 50.sp,
        modifier = Modifier
            .height(50.dp)
            .background(Color.LightGray)
            .wrapContentHeight()
            .fillMaxWidth(),
        textAlign = TextAlign.Center

    )
    AndroidViewBinding(ForagePinPadExampleBinding::inflate) {
        myPinPad.setOnChangeEventListener {
            text.value = "·".repeat(it.length)
            // since the sample app supports both a keypad and
            // this PinEditText, we tell the sample app to
            // assume the user wants to use this Keypad each
            // time the user touches a button. It's possible
            // that the EditText get's selected as the current
            // PIN element so a user need only press another digit
            // to tell the sample app to consider (again) the
            // keypad and not the EditText
            withPinElementReference(myPinPad)
        }
    }
}

@Preview
@Composable
fun ComposableForagePinPadPreview() {
    ComposableForagePINEditText(
        withPinElementReference = {}
    )
}
