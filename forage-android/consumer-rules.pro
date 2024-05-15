# This ensures that the ProxyRequestObject property names (like card_number_token) are preserved
# and are not obfuscated when consumed by clients who use ProGuard.
-keepclassmembers class com.joinforage.forage.android.ecom.services.vault.bt.ProxyRequestObject {
    *;
}