package com.joinforage.forage.android.ui

import android.text.Editable
import android.text.TextWatcher
import com.joinforage.forage.android.model.StateIIN
import java.lang.Integer.min

class PanTextWatcher: TextWatcher {
    private var updatedText = ""
    private var editing = false
    override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {

    }

    private fun validateNewInput(text: String): Boolean {
        val stateInn = StateIIN.values().find {
            text.startsWith(it.iin)
        }
        return if (stateInn == null) {
            text.length <= 19
        } else {
            text.length <= stateInn.panLength
        }
    }

    private fun formatPan(text: String, maskToApply: Int): String {
        when (maskToApply) {
            16 -> {
                // Mask of #### #### #### ####
                val maskComponentLengths = listOf(4, 4, 4, 4, 3)
                return splitPan(text, maskComponentLengths).joinToString(separator = " ")
            }
            18 -> {
                // Mask of ###### #### ##### ## #
                val maskComponentLengths = listOf(6, 4, 5, 2, 1)
                return splitPan(text, maskComponentLengths).joinToString(separator = " ")
            }
            else -> {
                // Mask of ###### #### #### ### ##
                val maskComponentLengths = listOf(6, 4, 4, 3, 2)
                return splitPan(text, maskComponentLengths).joinToString(separator = " ")
            }
        }
    }

    private fun splitPan(text: String, sections: List<Int>): List<String> {
        val result = mutableListOf<String>()
        val maskIterator = sections.iterator()
        var toDeplete = text
        while (toDeplete.isNotEmpty()) {
            val maxSectionLength = maskIterator.next()
            val length = min(toDeplete.length, maxSectionLength)
            val section = toDeplete.substring(0, length)
            result.add(section)
            toDeplete = toDeplete.removeRange(0, length)
        }

        return result
    }

    override fun onTextChanged(text: CharSequence?, start: Int, before: Int, count: Int) {
        // THIS IS A LOCK
        if (editing) return

        // Note: My IDE yelled at me for using [^0-9] as the regex pattern and said I should use /D.
        // However, that pattern doesn't seem to validate correctly and the legacy pattern does.
         val pan = text.toString().replace(Regex("[^0-9]"), "")

        // Check if the current PAN is already at max length for a valid entry and cap the user there
        if (!validateNewInput(pan)) {
            return
        }

        var maskToApply = 16
        val stateInn = StateIIN.values().find {
            pan.startsWith(it.iin)
        }

        if (stateInn != null) {
            maskToApply = stateInn.panLength
        }

        val maskedPan = formatPan(pan, maskToApply)
        updatedText = maskedPan
    }

    override fun afterTextChanged(editable: Editable?) {
        // THIS IS A LOCK
        if (editing) return

        editing = true
        editable?.clear()
        editable?.insert(0, updatedText)

        editing = false
    }
}