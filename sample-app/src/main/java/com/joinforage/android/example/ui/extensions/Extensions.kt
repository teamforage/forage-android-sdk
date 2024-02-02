package com.joinforage.android.example.ui.extensions

import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag

internal fun Modifier.withTestId(testId: String): Modifier = this.then(
    semantics { testTag = testId; contentDescription = testId }
)