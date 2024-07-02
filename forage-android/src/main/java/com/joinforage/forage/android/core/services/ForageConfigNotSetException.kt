package com.joinforage.forage.android.core.services

/**
 * An [Exception](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-exception/) thrown if a
 * reference to a [ForageElement][com.joinforage.forage.android.core.ui.element.ForageElement] is passed to a
 * method before [setForageConfig][com.joinforage.forage.android.core.ui.element.ForagePanElement.setForageConfig]
 * is called on the Element.
 * @property message A string that describes the Exception.
 */
class ForageConfigNotSetException(override val message: String) : IllegalStateException(message)
