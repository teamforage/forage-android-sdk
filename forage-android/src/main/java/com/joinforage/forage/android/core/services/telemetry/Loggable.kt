package com.joinforage.forage.android.core.services.telemetry

internal interface ILoggableAttributes {
    fun toMap(): Map<String, String>
}

internal sealed class Loggable(
    val prefix: String,
    val msg: String,
    val attrs: Map<String, String>
) {

    override fun toString() = "$prefix\t$msg"

    class Debug(prefix: String, msg: String, attrs: Map<String, String>) :
        Loggable(prefix, msg, attrs)
    class Info(prefix: String, msg: String, attrs: Map<String, String>) :
        Loggable(prefix, msg, attrs)
    class Warn(prefix: String, msg: String, attrs: Map<String, String>) :
        Loggable(prefix, msg, attrs)
    class Error(prefix: String, msg: String, val throwable: Throwable? = null, attrs: Map<String, String>) :
        Loggable(prefix, msg, attrs) {
        override fun toString() = "$prefix\t$msg\n\n$throwable"
    }

    class Metric(prefix: String, msg: String, attrs: Map<String, String>) :
        Loggable(
            "$prefix[Metric][${attrs[MetricAttributes.AttributeKey.EVENT_NAME.key]}]",
            msg,
            attrs
        )

    // Useful for debugging tests!!
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null) return false
        if (this::class != other::class) return false

        other as Loggable
        if (prefix != other.prefix) return false
        if (msg != other.msg) return false
        // Special handling for Error class
        if (this is Error && other is Error) {
            if (throwable == null) return other.throwable == null
            if (other.throwable == null) return false
            return throwable::class == other.throwable::class &&
                throwable.message == other.throwable.message
        }

        var thisAttrs = attrs
        var otherAttrs = other.attrs

        // Special handling for Metric class
        if (this is Metric && other is Metric) {
            thisAttrs = attrs.filterKeys {
                it != MetricAttributes.AttributeKey.RESPONSE_TIME_MS.key
            }
            otherAttrs = other.attrs.filterKeys {
                it != MetricAttributes.AttributeKey.RESPONSE_TIME_MS.key
            }
        }

        // Debug Map differences
        if (thisAttrs != otherAttrs) {
            println("Maps are different:")

            // Print keys only in thisAttrs
            thisAttrs.keys.filterNot { it in otherAttrs.keys }.forEach { k ->
                println("Key '$k' only in first map with value: ${thisAttrs[k]}")
            }

            // Print keys only in otherAttrs
            otherAttrs.keys.filterNot { it in thisAttrs.keys }.forEach { k ->
                println("Key '$k' only in second map with value: ${otherAttrs[k]}")
            }

            // Print different values for shared keys
            thisAttrs.forEach { (k, v) ->
                if (k in otherAttrs && otherAttrs[k] != v) {
                    println("Value different for key '$k': '$v' vs '${otherAttrs[k]}'")
                }
            }
            return false
        }
        return true
    }

    // we don't care about hash codes. We only implement this
    // because we modified the equals method
    override fun hashCode(): Int {
        var result = prefix.hashCode()
        result = 31 * result + msg.hashCode()
        result = 31 * result + attrs.hashCode()
        if (this is Error) {
            result = 31 * result + (throwable?.let { it::class.hashCode() * 31 + (it.message?.hashCode() ?: 0) } ?: 0)
        }
        return result
    }
}
