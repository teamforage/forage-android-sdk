package com.joinforage.forage.android.core.ui.element

internal typealias SimpleElementListener = () -> Unit
internal typealias StatefulElementListener<T> = (state: T) -> Unit
