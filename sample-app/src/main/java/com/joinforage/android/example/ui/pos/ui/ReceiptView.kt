package com.joinforage.android.example.ui.pos.ui

import android.content.Context
import android.graphics.Paint
import android.graphics.Typeface
import android.text.SpannableString
import android.text.style.UnderlineSpan
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import com.joinforage.android.example.R
import com.joinforage.android.example.pos.receipts.LinePartAlignment
import com.joinforage.android.example.pos.receipts.ReceiptLayout
import com.joinforage.android.example.pos.receipts.ReceiptLayoutLine
import com.joinforage.android.example.pos.receipts.ReceiptLinePart
import com.joinforage.android.example.pos.receipts.ReceiptPrinter
import com.pos.sdk.DevicesFactory

internal fun createReceiptPartTextView(context: Context, part: ReceiptLinePart) = TextView(context).apply {
    layoutParams = LinearLayout.LayoutParams(
        0,
        LinearLayout.LayoutParams.WRAP_CONTENT,
        part.colWeight
    )
    gravity = when (part.alignment) {
        LinePartAlignment.LEFT -> Gravity.START
        LinePartAlignment.CENTER -> Gravity.CENTER_HORIZONTAL
        LinePartAlignment.RIGHT -> Gravity.END
    }

    // extract commonly used variables
    val format = part.format
    val content = part.content

    // take measures to support underline text
    val spannableString = SpannableString(content)
    if (format.isUnderLine) {
        spannableString.setSpan(UnderlineSpan(), 0, content.length, 0)
    }

    // set the text
    text = spannableString

    // handle remaining formatting
    setLineSpacing(format.lineSpace.toFloat(), 1f)
    setTypeface(
        null,
        when {
            format.isBold && format.isItalic -> Typeface.BOLD_ITALIC
            format.isBold -> Typeface.BOLD
            format.isItalic -> Typeface.ITALIC
            else -> Typeface.NORMAL
        }
    )
    if (format.isStrikeThruText) {
        paintFlags = paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
    }

    // fonts appear much larger on the screen than on the receipt
    // so we shrink the font size on the display to keep the sizes
    // similar
    val adjustedFontSize = format.textSize.toFloat() / 2
    textSize = adjustedFontSize
}

internal fun createReceiptLineLinearLayout(context: Context, line: ReceiptLayoutLine) = LinearLayout(context).apply {
    line.parts.forEach { part ->
        val textView = createReceiptPartTextView(context, part)
        addView(textView)
    }
}

internal fun createReceiptDisplay(context: Context, receiptLayout: ReceiptLayout) = LinearLayout(context).apply {
    orientation = LinearLayout.VERTICAL

    layoutParams = LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.MATCH_PARENT,
        LinearLayout.LayoutParams.WRAP_CONTENT
    )

    // Set background color
    val backgroundColor = ContextCompat.getColor(context, R.color.light_grey)
    setBackgroundColor(backgroundColor)

    // Convert 8dp padding to pixels
    val paddingInPixels = (8 * context.resources.displayMetrics.density).toInt()
    setPadding(paddingInPixels, paddingInPixels, paddingInPixels, paddingInPixels)

    // add the TextViews that make up the ReceiptDisplay's content
    receiptLayout.lines.forEach { line ->
        val lineLayout = createReceiptLineLinearLayout(context, line)
        addView(lineLayout)
    }
}

class ReceiptView(
    context: Context
) : ScrollView(context) {
    private val scrollableContent: LinearLayout
    private val printReceiptBtn: Button
    private var receiptDisplay: LinearLayout
    private var receiptLayout = ReceiptLayout.EmptyReceiptLayout

    init {
        // organize the scrollview itself will house (the root)
        // which will house the container that will overflow as scroll
        layoutParams = LinearLayout.LayoutParams(
            ConstraintLayout.LayoutParams.MATCH_PARENT,
            ConstraintLayout.LayoutParams.WRAP_CONTENT
        )
        val rootPaddingInPixels = (16 * context.resources.displayMetrics.density).toInt()
        setPadding(rootPaddingInPixels, rootPaddingInPixels, rootPaddingInPixels, rootPaddingInPixels)

        // organize and add the subviews
        scrollableContent = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )

            printReceiptBtn = Button(context).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                text = "Print Receipt"

                // when the button is clicked, have it pass the current
                // receiptLayout (datastructure) to our ReceiptPrinter
                // service so it so we can print it on the POS terminal
                setOnClickListener {
                    val cpayPrinter = DevicesFactory.getDeviceManager().printDevice
                    ReceiptPrinter(receiptLayout).printWithCPayTerminal(cpayPrinter)
                }
            }
            addView(printReceiptBtn)

            // organize the linear layout that will hold the receipt details
            receiptDisplay = createReceiptDisplay(context, receiptLayout)
            addView(receiptDisplay)
        }
        addView(scrollableContent)
    }

    internal fun setReceiptLayout(newReceiptLayout: ReceiptLayout) {
        // remove the old view if it exists
        scrollableContent.removeView(receiptDisplay)
        // set the new receiptLayout value
        receiptLayout = newReceiptLayout
        // create the new receiptView and save it
        val newReceiptDisplay = createReceiptDisplay(context, newReceiptLayout)
        // set the new receiptView field
        receiptDisplay = newReceiptDisplay
        // add the new receiptView to the parent view
        scrollableContent.addView(receiptDisplay)
    }
}
