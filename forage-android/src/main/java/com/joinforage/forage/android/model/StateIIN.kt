package com.joinforage.forage.android.model

internal const val STATE_INN_LENGTH = 6

enum class USState(val abbreviation: String) {
    ALABAMA("AL"),
    ALASKA("AK"),
    ARIZONA("AZ"),
    ARKANSAS("AR"),
    CALIFORNIA("CA"),
    COLORADO("CO"),
    CONNECTICUT("CT"),
    DELAWARE("DE"),
    DISTRICT_OF_COLUMBIA("DC"),
    FLORIDA("FL"),
    GEORGIA("GA"),
    GUAM("GU"),
    HAWAII("HI"),
    IDAHO("ID"),
    ILLINOIS("IL"),
    INDIANA("IN"),
    IOWA("IA"),
    KANSAS("KS"),
    KENTUCKY("KY"),
    LOUISIANA("LA"),
    MAINE("ME"),
    MARYLAND("MD"),
    MASSACHUSETTS("MA"),
    MICHIGAN("MI"),
    MINNESOTA("MN"),
    MISSISSIPPI("MS"),
    MISSOURI("MO"),
    MONTANA("MT"),
    NEBRASKA("NE"),
    NEVADA("NV"),
    NEW_HAMPSHIRE("NH"),
    NEW_JERSEY("NJ"),
    NEW_MEXICO("NM"),
    NEW_YORK("NY"),
    NORTH_CAROLINA("NC"),
    NORTH_DAKOTA_SOUTH_DAKOTA("ND/SD"),
    OHIO("OH"),
    OKLAHOMA("OK"),
    OREGON("OR"),
    PENNSYLVANIA("PA"),
    RHODE_ISLAND("RI"),
    SOUTH_CAROLINA("SC"),
    TENNESSEE("TN"),
    TEXAS("TX"),
    US_VIRGIN_ISLANDS("VI"),
    UTAH("UT"),
    VERMONT("VT"),
    VIRGINIA("VA"),
    WASHINGTON("WA"),
    WEST_VIRGINIA("WV"),
    WISCONSIN("WI"),
    WYOMING("WY")
}

internal enum class StateIIN(
    val iin: String,
    val panLength: Int,
    val publicEnum: USState
) {
    ALABAMA("507680", 16, USState.ALABAMA),
    ALASKA("507695", 16, USState.ALASKA),
    ARIZONA("507706", 16, USState.ARIZONA),
    ARKANSAS("610093", 16, USState.ARKANSAS),
    CALIFORNIA("507719", 16, USState.CALIFORNIA),
    COLORADO("507681", 16, USState.COLORADO),
    CONNECTICUT("600890", 18, USState.CONNECTICUT),
    DELAWARE("507713", 16, USState.DELAWARE),
    DISTRICT_OF_COLUMBIA("507707", 16, USState.DISTRICT_OF_COLUMBIA),
    FLORIDA("508139", 16, USState.FLORIDA),
    GEORGIA("508148", 16, USState.GEORGIA),
    GUAM("578036", 16, USState.GUAM),
    HAWAII("507698", 16, USState.HAWAII),
    IDAHO("507692", 16, USState.IDAHO),
    ILLINOIS("601453", 16, USState.ILLINOIS),
    INDIANA("507704", 16, USState.INDIANA),
    IOWA("627485", 19, USState.IOWA),
    KANSAS("601413", 16, USState.KANSAS),
    KENTUCKY("507709", 16, USState.KENTUCKY),
    LOUISIANA("504476", 16, USState.LOUISIANA),
    MAINE("507703", 19, USState.MAINE),
    MARYLAND("600528", 16, USState.MARYLAND),
    MASSACHUSETTS("600875", 18, USState.MASSACHUSETTS),
    MICHIGAN("507711", 16, USState.MICHIGAN),
    MINNESOTA("610423", 16, USState.MINNESOTA),
    MISSISSIPPI("507718", 16, USState.MISSISSIPPI),
    MISSOURI("507683", 16, USState.MISSOURI),
    MONTANA("507714", 16, USState.MONTANA),
    NEBRASKA("507716", 16, USState.NEBRASKA),
    NEVADA("507715", 16, USState.NEVADA),
    NEW_HAMPSHIRE("507701", 16, USState.NEW_HAMPSHIRE),
    NEW_JERSEY("610434", 16, USState.NEW_JERSEY),
    NEW_MEXICO("586616", 16, USState.NEW_MEXICO),
    NEW_YORK("600486", 19, USState.NEW_YORK),
    NORTH_CAROLINA("508161", 16, USState.NORTH_CAROLINA),
    NORTH_DAKOTA_SOUTH_DAKOTA("508132", 16, USState.NORTH_DAKOTA_SOUTH_DAKOTA),
    OHIO("507700", 16, USState.OHIO),
    OKLAHOMA("508147", 16, USState.OKLAHOMA),
    OREGON("507693", 16, USState.OREGON),
    PENNSYLVANIA("600760", 19, USState.PENNSYLVANIA),
    RHODE_ISLAND("507682", 16, USState.RHODE_ISLAND),
    SOUTH_CAROLINA("610470", 16, USState.SOUTH_CAROLINA),
    TENNESSEE("507702", 16, USState.TENNESSEE),
    TEXAS("610098", 19, USState.TEXAS),
    US_VIRGIN_ISLANDS("507721", 16, USState.US_VIRGIN_ISLANDS),
    UTAH("601036", 16, USState.UTAH),
    VERMONT("507705", 16, USState.VERMONT),
    VIRGINIA("622044", 16, USState.VIRGINIA),
    WASHINGTON("507710", 16, USState.WASHINGTON),
    WEST_VIRGINIA("507720", 16, USState.WEST_VIRGINIA),
    WISCONSIN("507708", 16, USState.WISCONSIN),
    WYOMING("505349", 16, USState.WYOMING)
}

internal fun missingStateIIN(cardNumber: String): Boolean {
    return cardNumber.length < STATE_INN_LENGTH
}
internal fun queryForStateIIN(cardNumber: String): StateIIN? {
    return StateIIN.values().find { cardNumber.startsWith(it.iin) }
}
internal fun hasInvalidStateIIN(cardNumber: String): Boolean {
    return queryForStateIIN(cardNumber) == null
}
internal fun tooShortForStateIIN(cardNumber: String): Boolean {
    val iin = queryForStateIIN(cardNumber) ?: return true
    return cardNumber.length < iin.panLength
}
internal fun tooLongForStateIIN(cardNumber: String): Boolean {
    val iin = queryForStateIIN(cardNumber) ?: return true
    return cardNumber.length > iin.panLength
}
internal fun isCorrectLength(cardNumber: String): Boolean {
    return !tooShortForStateIIN(cardNumber) && !tooLongForStateIIN(cardNumber)
}
