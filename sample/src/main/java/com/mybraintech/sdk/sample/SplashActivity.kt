package com.mybraintech.sdk.sample

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import timber.log.Timber

class SplashActivity : AppCompatActivity() {

    companion object {
        const val REQUEST_CODE_PERMISSIONS = 1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_new_sdk)

        requestPermissions()

        findViewById<View>(R.id.btn_melomind).setOnClickListener {
            requestPermissions(
                Intent(this, MainActivity::class.java)
                    .apply {
                        putExtra(MainActivity.DEVICE_TYPE_KEY, MainActivity.MELOMIND_DEVICE)
                    })
        }

        findViewById<View>(R.id.btn_q_plus).setOnClickListener {
            requestPermissions(
                Intent(this, MainActivity::class.java)
                    .apply {
                        putExtra(MainActivity.DEVICE_TYPE_KEY, MainActivity.Q_PLUS_DEVICE)
                    })
        }

    }

    private fun requestPermissions(activityIntent: Intent? = null) {
        var permissions =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                Timber.w("requires BLUETOOTH_CONNECT and BLUETOOTH_SCAN")
                arrayOf(
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.BLUETOOTH_SCAN
                )
            } else {
                arrayOf(
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.BLUETOOTH_ADMIN,
                    Manifest.permission.BLUETOOTH,
                )
            }

        if (!hasPermissions(this, permissions)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(permissions, REQUEST_CODE_PERMISSIONS)
            }
        } else {
            Timber.i("launch activity")
            activityIntent?.let {
                startActivity(it)
            }
        }
    }

    private fun hasPermissions(context: Context, permissions: Array<String>): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            for (permission in permissions) {
                if (ActivityCompat.checkSelfPermission(
                        context,
                        permission
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    return false
                }
            }
        }
        return true
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            requestPermissions() // request permissions util all is permitted
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }

}