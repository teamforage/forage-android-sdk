package com.joinforage.android.example.ui.extensions

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.semantics.testTagsAsResourceId

@OptIn(ExperimentalComposeUiApi::class)
internal fun Modifier.withTestId(testId: String): Modifier = this.then(
    // inspired by: https://github.com/appium/appium/issues/15138#issuecomment-1346646361
    semantics { testTagsAsResourceId = true; testTag = "com.joinforage.android.example:id/$testId"; contentDescription = testId }
)
