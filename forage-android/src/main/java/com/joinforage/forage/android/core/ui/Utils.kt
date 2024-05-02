package com.joinforage.forage.android.core.ui

import android.content.res.TypedArray
import android.content.Context
import android.util.TypedValue

internal fun getThemeAccentColor(context: Context): Int {
    val outValue = TypedValue()
    context.theme.resolveAttribute(android.R.attr.colorAccent, outValue, true)
    return outValue.data
}
internal fun TypedArray.getBoxCornerRadiusBottomStart(boxCornerRadius: Float): Float {
    val boxCornerRadiusBottomStart =
        getDimension(com.joinforage.forage.android.R.styleable.ForagePANEditText_boxCornerBottomStart, 0f)
    return if (boxCornerRadiusBottomStart == 0f) boxCornerRadius else boxCornerRadiusBottomStart
}
internal fun TypedArray.getBoxCornerRadiusTopEnd(boxCornerRadius: Float): Float {
    val boxCornerRadiusTopEnd =
        getDimension(com.joinforage.forage.android.R.styleable.ForagePANEditText_boxCornerTopEnd, 0f)
    return if (boxCornerRadiusTopEnd == 0f) boxCornerRadius else boxCornerRadiusTopEnd
}
internal fun TypedArray.getBoxCornerRadiusBottomEnd(boxCornerRadius: Float): Float {
    val boxCornerRadiusBottomEnd =
        getDimension(com.joinforage.forage.android.R.styleable.ForagePANEditText_boxCornerBottomEnd, 0f)
    return if (boxCornerRadiusBottomEnd == 0f) boxCornerRadius else boxCornerRadiusBottomEnd
}
internal fun TypedArray.getBoxCornerRadiusTopStart(boxCornerRadius: Float): Float {
    val boxCornerRadiusTopStart =
        getDimension(com.joinforage.forage.android.R.styleable.ForagePANEditText_boxCornerTopStart, 0f)
    return if (boxCornerRadiusTopStart == 0f) boxCornerRadius else boxCornerRadiusTopStart
}

internal fun TypedArray.getBoxCornerRadius(styleIndex: Int, defaultBoxCornerRadius: Float): Float {
    val styledBoxCornerRadius = getDimension(styleIndex, 0f)
    return if (styledBoxCornerRadius == 0f) defaultBoxCornerRadius else styledBoxCornerRadius
}