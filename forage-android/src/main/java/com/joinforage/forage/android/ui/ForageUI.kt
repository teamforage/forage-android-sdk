package com.joinforage.forage.android.ui

import android.graphics.Typeface

interface ForageUI {
    var isValid: Boolean
    var isEmpty: Boolean
    var typeface: Typeface?
    fun setTextColor(textColor: Int)
    fun setTextSize(textSize: Float)
    fun setHint(hint: String)
    fun setHintTextColor(hintTextColor: Int)
}