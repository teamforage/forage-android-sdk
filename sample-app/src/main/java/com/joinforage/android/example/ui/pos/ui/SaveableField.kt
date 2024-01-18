package com.joinforage.android.example.ui.pos.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType

@Composable
fun SaveableField(
    initialValue: String,
    labelText: String,
    onSaveButtonClicked: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var inputValue by rememberSaveable {
        mutableStateOf(initialValue)
    }

    Column(
        modifier = modifier
    ) {
        TextField(
            value = inputValue,
            onValueChange = { inputValue = it },
            label = { Text(labelText) },
            keyboardOptions = KeyboardOptions.Default.copy(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Done
            )
        )
        Button(onClick = { onSaveButtonClicked(inputValue) }) {
            Text(text = "Save $labelText")
        }
    }
}