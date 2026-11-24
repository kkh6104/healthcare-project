package com.gh.healthapp.walk

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

/** 걷기 기록에 필요한 런타임 권한 (백그라운드 위치 권한은 요청하지 않음) */
object WalkPermissions {

    /** SDK 버전에 맞는 권한 목록. rememberLauncherForActivityResult(RequestMultiplePermissions)에 그대로 넘기면 된다 */
    fun required(): Array<String> = buildList {
        add(Manifest.permission.ACCESS_FINE_LOCATION)
        add(Manifest.permission.ACCESS_COARSE_LOCATION)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) add(Manifest.permission.ACTIVITY_RECOGNITION)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) add(Manifest.permission.POST_NOTIFICATIONS)
    }.toTypedArray()

    private fun granted(context: Context, permission: String) =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

    /** 아직 허용되지 않은 권한 */
    fun missing(context: Context): List<String> = required().filter { !granted(context, it) }

    /** 필요한 권한이 모두 허용되어 있는지 */
    fun hasAll(context: Context): Boolean = missing(context).isEmpty()

    /** 위치 권한(정확한 위치)만 확인 — 이게 없으면 기록을 시작할 수 없음 */
    fun hasLocation(context: Context): Boolean = granted(context, Manifest.permission.ACCESS_FINE_LOCATION)
}
