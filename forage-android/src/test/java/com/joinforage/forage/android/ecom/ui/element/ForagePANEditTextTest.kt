package com.joinforage.forage.android.ecom.ui.element

import com.joinforage.forage.android.mock.TestForagePANEditText
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ForagePANEditTextTest {

    val foragePANEditText: ForagePANEditText by lazy { TestForagePANEditText().create() }

    @Before
    fun before() = foragePANEditText.clearText()

    @Test
    fun `Verify setBoxStrokeWidthFocused`() {
        val randomWidth = 4321
        assertThat(foragePANEditText.textInputLayout.boxStrokeWidthFocused).isNotEqualTo(randomWidth)
        foragePANEditText.setBoxStrokeWidthFocused(randomWidth)
        assertThat(foragePANEditText.textInputLayout.boxStrokeWidthFocused).isEqualTo(randomWidth)
    }
}
