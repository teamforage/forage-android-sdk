## IF YOU WANT TO ENABLE PROGUARD IN FORAGE-SDK, UNCOMMENT THESE DIRECTIVES

# Keep the StringConcatFactory class and its methods
#-keep class java.lang.invoke.StringConcatFactory {
#    *;
#}
#
## Keep the TimeInfo class and its methods
#-keep class com.joinforage.datadog.android.api.context.TimeInfo {
#    *;
#}
#
#-dontwarn java.lang.invoke.StringConcatFactory