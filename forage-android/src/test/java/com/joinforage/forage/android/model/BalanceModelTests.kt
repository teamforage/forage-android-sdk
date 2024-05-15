package com.joinforage.forage.android.model

import com.joinforage.forage.android.core.services.forageapi.network.ForageApiResponse
import com.joinforage.forage.android.core.services.forageapi.paymentmethod.EbtBalance
import org.assertj.core.api.Assertions.assertThat
import org.json.JSONObject
import org.junit.Test

class BalanceModelTests {
    @Test
    fun `test primary constructor`() {
        val snap = "12.34"
        val nonSnap = "5.67"

        val balance = EbtBalance(snap, nonSnap)

        assert(balance.snap == snap)
        assert(balance.cash == nonSnap)
    }

    @Test
    fun `test secondary constructor`() {
        val jsonString = """
            {
                "snap": "1.23",
                "non_snap": "1004.56"
            }
        """.trimIndent()

        val balance = EbtBalance(JSONObject(jsonString))

        assert(balance.snap == "1.23")
        assert(balance.cash == "1004.56")
    }

    @Test
    fun `test toString()`() {
        val snap = "12.34"
        val nonSnap = "5.67"

        val result = EbtBalance(snap, nonSnap).toString()

        assert(result == "{\"snap\":\"$snap\",\"cash\":\"$nonSnap\"}")
    }

    @Test
    fun `test fromSdkResponse()`() {
        val snapBalance = "1.23"
        val ebtCashBalance = "1004.56"
        val jsonString = """
            {
                "snap": "$snapBalance",
                "cash": "$ebtCashBalance"
            }
        """.trimIndent()

        val balance = EbtBalance.fromSdkResponse(jsonString)

        assert(balance.snap == snapBalance)
        assert(balance.cash == ebtCashBalance)
    }

    @Test
    fun `test ForageApiResponse toBalance()`() {
        val snapBalance = "1.23"
        val ebtCashBalance = "1004.56"
        val jsonString = """
            {
                "snap": "$snapBalance",
                "cash": "$ebtCashBalance"
            }
        """.trimIndent()

        val apiResponse = ForageApiResponse.Success(jsonString)
        val balance = apiResponse.toBalance()
        assert(balance is EbtBalance)

        val ebtBalance = balance as EbtBalance
        assertThat(ebtBalance.snap).isEqualTo(snapBalance)
        assertThat(ebtBalance.cash).isEqualTo(ebtCashBalance)
    }
}
