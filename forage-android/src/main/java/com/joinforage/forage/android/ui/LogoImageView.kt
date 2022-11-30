package com.joinforage.forage.android.ui

import android.content.Context
import android.view.Gravity
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.ImageView
import android.widget.LinearLayout
import com.joinforage.forage.android.R

/**
 * ImageView containing the Forage logo
 */
fun getLogoImageViewLayout(context: Context): LinearLayout {
    val forageLogoHeight = context.resources.getDimensionPixelSize(R.dimen.forage_logo_height)

    val imageView = ImageView(context).apply {
        setImageResource(R.drawable.powered_by_forage_logo)
        layoutParams = LinearLayout.LayoutParams(
            MATCH_PARENT,
            forageLogoHeight
        )
    }

    val linearLayout = LinearLayout(context).apply {
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        gravity = Gravity.CENTER
        addView(imageView)
    }

    val leftMargin = context.resources.getDimensionPixelSize(R.dimen.forage_logo_left_margin)
    val topMargin = context.resources.getDimensionPixelSize(R.dimen.forage_logo_top_margin)
    val rightMargin = context.resources.getDimensionPixelSize(R.dimen.forage_logo_right_margin)
    val bottomMargin = context.resources.getDimensionPixelSize(R.dimen.forage_logo_bottom_margin)

    linearLayout.setPadding(leftMargin, topMargin, rightMargin, bottomMargin)

    return linearLayout
}
