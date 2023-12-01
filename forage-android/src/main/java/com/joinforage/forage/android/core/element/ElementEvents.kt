package com.joinforage.forage.android.core.element

import com.joinforage.forage.android.core.element.state.ElementState

internal typealias SimpleElementListener = () -> Unit
internal typealias StatefulElementListener<InputDetails> = (state: ElementState<InputDetails>) -> Unit
