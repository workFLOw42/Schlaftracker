package de.schlafgut.app.util

import java.util.Locale

/** Locale-safe String.format using US locale for numeric consistency. */
fun fmt(pattern: String, vararg args: Any?): String = String.format(Locale.US, pattern, *args)
