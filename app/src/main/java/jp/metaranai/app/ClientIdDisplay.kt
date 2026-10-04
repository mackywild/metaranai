package jp.metaranai.app

// Presentation only: retain the original ID for authorization.
fun maskClientId(value: String): String = value.take(4) + "•".repeat((value.length - 4).coerceAtLeast(0))
