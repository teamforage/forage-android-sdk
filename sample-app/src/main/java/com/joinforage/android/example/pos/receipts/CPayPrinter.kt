package com.joinforage.android.example.pos.receipts

import android.os.Bundle
import android.util.Log
import com.joinforage.android.example.pos.receipts.primitives.LinePartAlignment
import com.joinforage.android.example.pos.receipts.primitives.ReceiptLayout
import com.joinforage.android.example.pos.receipts.primitives.ReceiptLayoutLine
import com.joinforage.android.example.pos.receipts.primitives.ReceiptLinePart
import com.pos.sdk.printer.PrinterDevice
import com.pos.sdk.printer.param.MultipleTextPrintItemParam
import com.pos.sdk.printer.param.PrintItemAlign
import com.pos.sdk.printer.param.TextPrintItemParam

// TODO: write tests for the classes and methods in this file
//  as they are business logic and are straightforward to test

/**
 * This class and the methods it uses effectively map
 * ReceiptLayouts, ReceiptLayoutLines, and ReceiptLineParts
 * to the data structures that the CPay SDK uses for printing
 */
internal class CPayPrinter(private val cpayPrinter: PrinterDevice) {
    fun setLayout(layout: ReceiptLayout) {
        // clear the buffer before setting anything new
        cpayPrinter.clearBufferArea()

        // transform and write lines to the CPay printer's buffer
        layout.lines.forEach {
            CPayLine.of(it).addLineToPrinter(cpayPrinter)
        }
    }
    fun print() {
        val state = cpayPrinter.printSync(Bundle())
        Log.i("CPay SDK", "result is ${state.stateCode},msg is ${state.stateMsg}")
    }
}

/**
 * An abstract class that represents an abstraction over
 * lines on a receipt in the CPay SDK. This makes it easier
 * to isolate and test business logic for handling the
 * difference between single and multi part receipt lines
 */
internal abstract class CPayLine(protected val line: ReceiptLayoutLine) {
    abstract fun addLineToPrinter(cpayPrinter: PrinterDevice)
    companion object {
        fun of(line: ReceiptLayoutLine): CPayLine {
            return if (line.parts.size == 1) {
                SingleCPayLine(line)
            } else {
                MultiCPayLine(line)
            }
        }
    }
}

internal class SingleCPayLine(line: ReceiptLayoutLine) : CPayLine(line) {
    override fun addLineToPrinter(cpayPrinter: PrinterDevice) {
        val single = processSinglePartLine(line)
        cpayPrinter.addTextPrintItem(single)
    }
}

internal class MultiCPayLine(line: ReceiptLayoutLine) : CPayLine(line) {
    override fun addLineToPrinter(cpayPrinter: PrinterDevice) {
        val multi = processMultiPartLine(line)
        cpayPrinter.addMultipleTextPrintItem(multi)
    }
}

/**
 * A function that maps ReceiptLayoutLines with more than 1 part into
 * CPay SDK representations of lines with more than one column.
 *
 * While TextPrintItemParam are the smallest representation in the
 * CPay SDK, MultipleTextPrintItemParam represent lines with multiple
 * parts are are composed of TextPrintItemParams
 */
internal fun processMultiPartLine(line: ReceiptLayoutLine): MultipleTextPrintItemParam {
    val scales = line.parts.map { part -> part.colWeight }.toFloatArray()
    val textPrintItems = line.parts.map { processLinePart(it) }.toTypedArray()
    return MultipleTextPrintItemParam(scales, textPrintItems)
}

/**
 * a function that maps ReceiptLayoutLInes with exactly 1 part
 * to CPay SDK representations of lines with exactly one column,
 * called TextPrintItemParam.
 */
internal fun processSinglePartLine(line: ReceiptLayoutLine): TextPrintItemParam {
    val part = line.parts[0]
    return processLinePart(part)
}

/**
 * Here the smallest units of our ReceiptLayout data structure
 * get transformed into the atomic units of receipts for the
 * CPay SDK. In our case, the atomic units of a receipt are
 * ReceiptLineParts. For CPay SDK, the atomic units are
 * TextPrintItemParam
 */
internal fun processLinePart(part: ReceiptLinePart): TextPrintItemParam {
    val item = TextPrintItemParam()

    // set the string content to be printed
    item.content = part.content

    // set any formatting of the string content
    item.textSize = part.format.textSize
    item.lineSpace = part.format.lineSpace
    item.isUnderLine = part.format.isUnderLine
    item.isBold = part.format.isBold
    item.isItalic = part.format.isItalic
    item.isStrikeThruText = part.format.isStrikeThruText

    // set the alignment
    when (part.alignment) {
        LinePartAlignment.LEFT -> item.itemAlign = PrintItemAlign.LEFT
        LinePartAlignment.CENTER -> item.itemAlign = PrintItemAlign.CENTER
        LinePartAlignment.RIGHT -> item.itemAlign = PrintItemAlign.RIGHT
    }

    // we're done
    return item
}
