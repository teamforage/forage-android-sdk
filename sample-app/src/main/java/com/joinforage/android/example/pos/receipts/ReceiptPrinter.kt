package com.joinforage.android.example.pos.receipts

import com.pos.sdk.printer.PrinterDevice

/**
 * A class that effectively transforms our internal representation
 * of a receipt (i.e. a ReceiptLayout), into the datastructures
 * that POS printers can understand. Right now it only supports
 * printing with the CPay SDK, which is used for our POS terminal
 */
internal class ReceiptPrinter(private val layout: ReceiptLayout) {
    internal fun printWithCPayTerminal(printer: PrinterDevice) {
        val cpayPrinter = CPayPrinter(printer)
        cpayPrinter.setLayout(layout)
        cpayPrinter.print()
    }
}
