package com.joinforage.forage.android.core.services.forageapi.network.error.payload

import com.joinforage.forage.android.core.services.forageapi.polling.ForageErrorDetails
import org.json.JSONObject

/**
 * Converts error responses passed as single errors to a [Failure] instance.
 * For example:
 *
 * {
 *   "ref": "e1fff94f29",
 *   "balance": null,
 *   "content_id": "c1898593-fa3d-4a1c-b16b-10ecc38b3619",
 *   "error": {
 *     "message": "Invalid card number - Re-enter Transaction",
 *     "forage_code": "ebt_error_14",
 *     "status_code": 400
 *   }
 * }
 */
internal open class SingleErrorResponsePayload(
    jsonErrorResponse: JSONObject
) : ErrorPayload(jsonErrorResponse) {
    // dynamic properties so that they do not throw an error
    // during constructor calling
    protected open val error: JSONObject
        get() = jsonErrorResponse.getJSONObject("error")
    private val code: String
        get() = parseCode()

    override fun parseCode(): String = error.getString("forage_code")
    override fun parseMessage(): String = error.getString("message")
    override fun parseDetails(): ForageErrorDetails? = ForageErrorDetails.from(code, error)
}

/**
 * Converts Refund error responses passed to a [Failure] instance.
 * For example:
 *
 * {
 *   "ref": "591e7352db",
 *   "payment_ref": "0c70c9881a",
 *   "funding_type": "ebt_cash",
 *   "amount": "1.00",
 *   "reason": "test payment",
 *   "metadata": {},
 *   "created": "2024-11-28T06:53:55.676413-08:00",
 *   "updated": "2024-11-28T06:53:55.975640-08:00",
 *   "status": "failed",
 *   "last_processing_error": null,
 *   "receipt": {
 *     "ref_number": "591e7352db",
 *     "is_voided": false,
 *     "snap_amount": "0.00",
 *     "ebt_cash_amount": "1.00",
 *     "cash_back_amount": "0.00",
 *     "other_amount": "0.00",
 *     "sales_tax_applied": "0.00",
 *     "balance": {
 *       "id": 785090,
 *       "snap": "100.00",
 *       "non_snap": "94.00",
 *       "updated": "2024-11-28T06:53:07.350899-08:00"
 *     },
 *     "last_4": "7777",
 *     "message": "Invalid PIN or PIN not selected - Invalid PIN",
 *     "transaction_type": "Refund",
 *     "created": "2024-11-28T06:53:55.970481-08:00",
 *     "sequence_number": "RE015d74"
 *   },
 *   "pos_terminal": {
 *     "terminal_id": "9ef04802",
 *     "provider_terminal_id": "HeadlessAndroidIntegrationTests"
 *   },
 *   "sequence_number": "RE015d74",
 *   "previous_errors": [
 *     {
 *       "code": "ebt_error_55",
 *       "message": "Invalid PIN or PIN not selected - Invalid PIN",
 *       "source": {
 *         "resource": "Refunds",
 *         "ref": "591e7352db"
 *       }
 *     }
 *   ],
 *   "refund_errors": [
 *     {
 *       "code": "ebt_error_55",
 *       "message": "Invalid PIN or PIN not selected - Invalid PIN",
 *       "source": {
 *         "resource": "Refunds",
 *         "ref": "591e7352db"
 *       }
 *     }
 *   ],
 *   "message": {
 *     "content_id": "8f333099-4e5d-4514-86b5-e927464659bf",
 *     "message_type": "0200",
 *     "status": "completed",
 *     "failed": true,
 *     "errors": [
 *       {
 *         "message": "Invalid PIN or PIN not selected - Invalid PIN",
 *         "forage_code": "ebt_error_55",
 *         "status_code": 400
 *       }
 *     ]
 *   }
 * }
 */
internal class DeferredRefundErrorResponsePayload(
    jsonErrorResponse: JSONObject
) : SingleErrorResponsePayload(
    jsonErrorResponse
) {
    // dynamic properties so that they do not throw an error
    // during constructor calling
    override val error: JSONObject
        get() = jsonErrorResponse
            .getJSONObject("message")
            .getJSONArray("errors")
            .get(0) as JSONObject
}
