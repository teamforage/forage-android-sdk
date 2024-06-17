package com.joinforage.forage.android.ui

import com.joinforage.forage.android.core.ui.element.ForageConfig
import com.joinforage.forage.android.core.ui.element.ForageConfigManager
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

val conf1 = ForageConfig("first", "config")
val conf2 = ForageConfig("second", "config")

class ForageConfigManagerTest {
    @Test
    fun `passing null does not invoke callback or change value`() {
        // Arrange
        var int = 0
        val manager = ForageConfigManager { int++ }
        assertThat(int).isEqualTo(0)
        assertThat(manager.forageConfig).isNull()

        // Act and Assert
        manager.forageConfig = null
        assertThat(int).isEqualTo(0) // callback not invoked when set to null
    }

    @Test
    fun `passing non-null changes value each time`() {
        // Arrange
        var int = 0
        val manager = ForageConfigManager { int++ }
        assertThat(int).isEqualTo(0)
        assertThat(manager.forageConfig).isNull()

        // Act and Assert
        manager.forageConfig = conf1
        assertThat(int).isEqualTo(1) // called when set to non-null
        assertThat(manager.forageConfig).isEqualTo(conf1)
    }

    @Test
    fun `passing non-null invokes callback only first time`() {
        // Arrange
        var int = 0
        val manager = ForageConfigManager { int++ }
        assertThat(int).isEqualTo(0)
        assertThat(manager.forageConfig).isNull()

        // Act and Assert
        manager.forageConfig = conf1
        manager.forageConfig = conf2
        assertThat(int).isEqualTo(1) // callback only gets called once
        assertThat(manager.forageConfig).isEqualTo(conf2) // config still updated
    }
}
