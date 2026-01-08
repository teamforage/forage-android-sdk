package com.joinforage.forage.android.core.services.forageapi.polling

import org.json.JSONObject
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ForageErrorDetailsTest {
    @Test
    fun `parses insufficient funds with balance amounts`() {
        val errorDetailsString = """{ "snap_balance": "10.00", "cash_balance": "20.00" }"""
        val errorDetailsJson = JSONObject(errorDetailsString)
        val result = ForageErrorDetails.EbtError51Details(errorDetailsJson)
        assertEquals(result.snapBalance, "10.00")
        assertEquals(result.cashBalance, "20.00")
        assertEquals(result.toString(), "Cash Balance: 20.00\nSNAP Balance: 10.00")
    }

    @Test
    fun `parses insufficient funds with null balance amounts`() {
        val errorDetailsString = """{ "snap_balance": null, "cash_balance": null }"""
        val errorDetailsJson = JSONObject(errorDetailsString)
        val result = ForageErrorDetails.EbtError51Details(errorDetailsJson)
        assertNull(result.snapBalance)
        assertNull(result.cashBalance)
        assertEquals(result.toString(), "Cash Balance: null\nSNAP Balance: null")
    }
}
