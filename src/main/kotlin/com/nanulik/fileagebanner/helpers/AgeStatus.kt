package com.nanulik.fileagebanner.helpers

/**
 * @author Nane Petrosyan
 * 11.02.26
 */
enum class AgeStatus(val label: String) {
    NEWBORN("newborn 👶"),
    FRESH("fresh 🌱"),
    OK("stable 🧘"),
    DUSTY("dusty 📦"),
    GERIATRIC("geriatric 🦕"),
    ANCIENT("ancient 🗿");

    companion object {
        fun fromDays(days: Long): AgeStatus = when {
            days < 1 -> NEWBORN
            days < 7 -> FRESH
            days < 30 -> OK
            days < 90 -> DUSTY
            days < 365 -> GERIATRIC
            else -> ANCIENT
        }
    }

    fun human(days: Long): String = when {
        days < 1 -> "today"
        days == 1L -> "1 day ago"
        else -> "$days days ago"
    }
}