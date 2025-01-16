package com.joinforage.forage.android.pos

import android.content.Context
import android.util.AttributeSet
import com.joinforage.forage.android.R
import com.joinforage.forage.android.core.ui.element.ForagePanElement

/**
 * A [ForagePanElement] that securely collects a customer's card number.
 * ```xml
 * <!-- Example forage_pan_component.xml -->
 * <?xml version="1.0" encoding="utf-8"?>
 * <androidx.constraintlayout.widget.ConstraintLayout xmlns:android="http://schemas.android.com/apk/res/android"
 *     xmlns:app="http://schemas.android.com/apk/res-auto"
 *     android:layout_width="match_parent"
 *     android:layout_height="match_parent">
 *
 *     <com.joinforage.forage.android.pos.ui.element.ForagePANEditText
 *             android:id="@+id/foragePanEditText"
 *             android:layout_width="0dp"
 *             android:layout_height="wrap_content"
 *             android:layout_margin="16dp"
 *             app:layout_constraintBottom_toBottomOf="parent"
 *             app:layout_constraintEnd_toEndOf="parent"
 *             app:layout_constraintStart_toStartOf="parent"
 *             app:layout_constraintTop_toTopOf="parent"
 *     />
 *
 * </androidx.constraintlayout.widget.ConstraintLayout>
 * ```
 * @see * [Guide to styling Forage Android Elements](https://docs.joinforage.app/docs/forage-android-styling-guide)
 * * [POS Terminal Android Quickstart](https://docs.joinforage.app/docs/forage-terminal-android)
 */
class ForagePANEditText @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = R.attr.foragePanEditTextStyle
) : ForagePanElement(context, attrs, defStyleAttr)
