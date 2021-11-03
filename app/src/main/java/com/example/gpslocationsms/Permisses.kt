package com.example.gpslocationsms

import android.Manifest
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts

class PermissionLocationRequester(
    activity: ComponentActivity,
    private val permission: String,
    private val onRationale: () -> Unit = {},
    private val onDenied: () -> Unit = {}
) {
    private var onGranted: () -> Unit = {}

    private val permissionLauncher = activity.registerForActivityResult(
        ActivityResultContracts.RequestPermission()){ isGranted ->
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            when{
                isGranted -> onGranted()
                activity.shouldShowRequestPermissionRationale(permission) ->
                    onRationale()
                else -> onDenied()
            }
        }
    }
    fun runWithPermission(body:() -> Unit){
        onGranted = body
        permissionLauncher.launch(permission)
    }
}

class PermissionSendMessageRequester(
    activity: ComponentActivity,
    private val permission: String,
    private val onRationale: () -> Unit = {},
    private val onDenied: () -> Unit = {}
) {
    private var onGranted: () -> Unit = {}
    private val permissionLauncher = activity.registerForActivityResult(
        ActivityResultContracts.RequestPermission()){ isGranted ->
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            when{
                isGranted -> onGranted()
                activity.shouldShowRequestPermissionRationale(permission) ->
                    onRationale()
                else -> onDenied()
            }
        }
    }
    fun runWithPermission(body:() -> Unit){
        onGranted = body
        permissionLauncher.launch(permission)
    }
}
