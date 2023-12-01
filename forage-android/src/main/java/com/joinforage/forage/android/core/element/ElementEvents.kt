package com.joinforage.forage.android.core.element

internal typealias SimpleElementListener = () -> Unit
internal typealias StatefulElementListener<T> = (state: T) -> Unit
