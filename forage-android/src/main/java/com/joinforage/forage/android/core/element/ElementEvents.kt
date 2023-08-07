package com.joinforage.forage.android.core.element

import com.joinforage.forage.android.core.element.state.ElementState

typealias SimpleElementListener = () -> Unit
typealias StatefulElementListener = (state: ElementState) -> Unit
