package com.example.myapplication

/** A sender can supply title/body, plus optional title_zu/body_zu and title_af/body_af. */
data class NotificationContent(val title: String, val body: String, val destination: String = "home") {
    companion object {
        fun from(
            data: Map<String, String>,
            language: String,
            defaultTitle: String,
            defaultBody: String,
            notificationTitle: String? = null,
            notificationBody: String? = null
        ): NotificationContent {
            fun field(name: String, notification: String?, fallback: String): String =
                sequenceOf(data["${name}_${language.substringBefore('-')}"], data[name], notification, fallback)
                    .filterNotNull().first { it.isNotBlank() }
            return NotificationContent(
                field("title", notificationTitle, defaultTitle),
                field("body", notificationBody, defaultBody),
                data["destination"].takeIf { it in setOf("home", "progress", "workout") } ?: "home"
            )
        }
    }
}
