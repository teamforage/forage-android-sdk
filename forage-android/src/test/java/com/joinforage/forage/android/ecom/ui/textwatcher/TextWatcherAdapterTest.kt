package com.joinforage.forage.android.ecom.ui.textwatcher

import com.joinforage.forage.android.core.ui.textwatcher.TextWatcherAdapter
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class TextWatcherAdapterTest {
    @Test
    fun `Verify all TextWatcherAdapter methods are callable`() {
        val textWatcher = TextWatcherAdapter()
        textWatcher.beforeTextChanged("", 0, 0, 0)
        textWatcher.onTextChanged("", 0, 0, 0)
        textWatcher.afterTextChanged(mock())
    }
}
