package com.joinforage.forage.android.model

import com.joinforage.forage.android.network.model.ForageErrorDetails
import com.joinforage.forage.android.network.model.SQSError
import org.assertj.core.api.Assertions.assertThat
import org.json.JSONObject
import org.junit.Test

class SQSErrorFromTest {

    @Test
    fun `passing JSON with NO returns null details`() {
        // Given
        val jsonString = """{
            "status_code": 420,
            "forage_code": "yeah_mon",
            "message": "I'm on one"
        }"""

        // When
        val result = SQSError.SQSErrorMapper.from(jsonString)

        // Then
        assertThat(result.statusCode).isEqualTo(420)
        assertThat(result.forageCode).isEqualTo("yeah_mon")
        assertThat(result.message).isEqualTo("I'm on one")
        assertThat(result.details).isNull()
    }

    @Test
    fun `passing JSON with details=null returns null details`() {
        // Given
        val jsonString = """{
            "status_code": 420,
            "forage_code": "yeah_mon",
            "message": "I'm on one",
            "details": null
        }"""

        // When
        val result = SQSError.SQSErrorMapper.from(jsonString)

        // Then
        assertThat(result.statusCode).isEqualTo(420)
        assertThat(result.forageCode).isEqualTo("yeah_mon")
        assertThat(result.message).isEqualTo("I'm on one")
        assertThat(result.details).isNull()
    }

    @Test
    fun `passing SQSError WITH arbitrary details returns null details`() {
        // Given
        val jsonString = """{
            "status_code": 400,
            "forage_code": "blah_blah",
            "message": "try again",
            "details": {
                "some_value": "hello",
            }
        }"""

        // When
        val result = SQSError.SQSErrorMapper.from(jsonString)

        // Then
        assertThat(result.statusCode).isEqualTo(400)
        assertThat(result.forageCode).isEqualTo("blah_blah")
        assertThat(result.message).isEqualTo("try again")
        assertThat(result.details).isNull()
    }

    @Test
    fun `handles ebt_error_51 correctly`() {
        // Given
        val jsonString = """{
            "status_code": 400,
            "forage_code": "ebt_error_51",
            "message": "Insufficient EBT Funds",
            "details": {
                "snap_balance": "10.00",
                "cash_balance": "5.00"
            }
        }"""

        // When
        val result = SQSError.SQSErrorMapper.from(jsonString)

        // Then
        assertThat(result.statusCode).isEqualTo(400)
        assertThat(result.forageCode).isEqualTo("ebt_error_51")
        assertThat(result.message).isEqualTo("Insufficient EBT Funds")

        // be cause we know we're dealing with ebt_error_51, we
        // can make this type assertion
        val details = result.details as ForageErrorDetails.EbtError51Details
        assertThat(details.snapBalance).isEqualTo("10.00")
        assertThat(details.cashBalance).isEqualTo("5.00")
    }
}

class InsufficientFundsDetailsTest {

    @Test
    fun `null details yields null balance fields`() {
        // Given
        val jsonObject = null

        // When
        val result = ForageErrorDetails.EbtError51Details.from(jsonObject)

        // Then
        assertThat(result.snapBalance).isNull()
        assertThat(result.cashBalance).isNull()
    }

    @Test
    fun `missing detail fields yields null for those fields`() {
        // Given
        val jsonObject = JSONObject("{}")

        // When
        val result = ForageErrorDetails.EbtError51Details.from(jsonObject)

        // Then
        assertThat(result.snapBalance).isNull()
        assertThat(result.cashBalance).isNull()
    }

    @Test
    fun `expected fields should work as expected`() {
        // Given
        val jsonObject = JSONObject(
            """{
                "snap_balance": "200.00",
                "cash_balance": "100.00"
            }"""
        )

        // When
        val result = ForageErrorDetails.EbtError51Details.from(jsonObject)

        // Then
        val details = result as ForageErrorDetails.EbtError51Details
        assertThat(details.snapBalance).isEqualTo("200.00")
        assertThat(details.cashBalance).isEqualTo("100.00")
    }
}
