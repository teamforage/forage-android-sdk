package com.joinforage.android.example.ui.pos.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.joinforage.android.example.ui.pos.ui.SaveableField

@Composable
fun ManualPANEntryScreen(
    initialValue: String,
    onSaveButtonClicked: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    SaveableField(
        initialValue = initialValue,
        labelText = "Card PAN",
        onSaveButtonClicked = onSaveButtonClicked,
        modifier = modifier
    )
}