package com.joinforage.android.example.ui.pos.screens.shared

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.joinforage.android.example.ui.pos.ui.ComposableForagePANEditText
import com.joinforage.android.example.ui.pos.ui.ErrorText
import com.joinforage.android.example.ui.pos.ui.ScreenWithBottomRow
import com.joinforage.forage.android.pos.PosForageConfig
import com.joinforage.forage.android.ui.ForagePANEditText

@Composable
fun ManualPANEntryScreen(
    posForageConfig: PosForageConfig,
    onSubmitButtonClicked: () -> Unit,
    onBackButtonClicked: () -> Unit,
    withPanElementReference: (element: ForagePANEditText) -> Unit,
    errorText: String? = null
) {
    ScreenWithBottomRow(
        mainContent = {
            Text("Manually enter your card number")
            Spacer(modifier = Modifier.height(8.dp))
            ComposableForagePANEditText(
                posForageConfig,
                withPanElementReference = withPanElementReference
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onSubmitButtonClicked) {
                Text("Submit")
            }
            Spacer(modifier = Modifier.height(16.dp))
            ErrorText(errorText)
        },
        bottomRowContent = {
            Button(onClick = onBackButtonClicked) {
                Text("Back")
            }
        }
    )
}

@Preview
@Composable
fun ManualPANEntryScreenPreview() {
    ManualPANEntryScreen(
        posForageConfig = PosForageConfig("", ""),
        onSubmitButtonClicked = {},
        onBackButtonClicked = {},
        withPanElementReference = {}
    )
}
