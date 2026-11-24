package com.gh.healthapp.ui.walk

import java.util.Locale

internal fun formatKm(distanceM: Double): String = String.format(Locale.KOREAN, "%.2f km", distanceM / 1000.0)

/** 평균 페이스 "6'05\"/km". 거리가 너무 짧으면 "-" */
internal fun formatPace(distanceM: Double, activeMs: Long): String {
    if (distanceM < 10.0 || activeMs <= 0) return "-"
    val secPerKm = (activeMs / 1000.0) / (distanceM / 1000.0)
    if (secPerKm > 99 * 60) return "-"
    val total = secPerKm.toInt()
    return String.format(Locale.KOREAN, "%d'%02d\"/km", total / 60, total % 60)
}
