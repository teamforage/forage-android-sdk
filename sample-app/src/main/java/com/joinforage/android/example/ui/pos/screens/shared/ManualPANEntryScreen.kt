package com.joinforage.android.example.ui.pos.screens.shared

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.joinforage.android.example.ui.extensions.withTestId
import com.joinforage.android.example.ui.pos.ui.ComposableForagePANEditText
import com.joinforage.android.example.ui.pos.ui.ErrorText
import com.joinforage.android.example.ui.pos.ui.ScreenWithBottomRow
import com.joinforage.forage.android.core.services.ForageConfig
import com.joinforage.forage.android.pos.ui.element.ForagePANEditText

@Composable
fun ManualPANEntryScreen(
    forageConfig: ForageConfig,
    onSubmitAsManualEntry: () -> Unit,
    onSubmitAsTrack2: () -> Unit,
    onBackButtonClicked: () -> Unit,
    withPanElementReference: (element: ForagePANEditText) -> Unit,
    errorText: String? = null
) {
    ScreenWithBottomRow(
        mainContent = {
            Text("Manually enter your card number")
            Spacer(modifier = Modifier.height(8.dp))
            ComposableForagePANEditText(
                forageConfig,
                withPanElementReference = withPanElementReference
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onSubmitAsManualEntry,
                modifier = Modifier.withTestId("pos_submit_button")
            ) {
                Text("Submit")
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onSubmitAsTrack2,
                modifier = Modifier.withTestId("pos_track_2_submit_button")
            ) {
                Text("Submit as Track 2")
            }
            Text("Track 2 will submit '<pan>=4912220'")
            Spacer(modifier = Modifier.height(16.dp))
            ErrorText(errorText)
        },
        bottomRowContent = {
            Button(
                onClick = onBackButtonClicked,
                modifier = Modifier.withTestId("pos_back_button")
            ) {
                Text("Back")
            }
        }
    )
}

@Preview
@Composable
fun ManualPANEntryScreenPreview() {
    ManualPANEntryScreen(
        forageConfig = ForageConfig("", ""),
        onSubmitAsManualEntry = {},
        onSubmitAsTrack2 = {},
        onBackButtonClicked = {},
        withPanElementReference = {}
    )
}
