package com.troca.latroca.utils

fun removeSpaces(text: String): String {
    return text.replace("\\s".toRegex(), "")
}

fun validateEmailInput(
    currentValue: String,
    newValue: String,
    maxLength: Int = 64
): String {
    val filtered = removeSpaces(newValue)
    return if (filtered.length <= maxLength) filtered else currentValue
}

fun validatePasswordInput(
    currentValue: String,
    newValue: String,
    maxLength: Int = 30
): String {
    val filtered = removeSpaces(newValue)
    return if (filtered.length <= maxLength) filtered else currentValue
}