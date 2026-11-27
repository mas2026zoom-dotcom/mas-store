package com.mas.store

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.mutableIntStateOf

class MainActivity : ComponentActivity() {
    private val tick = mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        Push.ensureChannel(this)
        Push.subscribe()
        if (Build.VERSION.SDK_INT >= 33) {
            registerForActivityResult(ActivityResultContracts.RequestPermission()) { }
                .launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        val tab = intent.getStringExtra("tab")
        val open = intent.getStringExtra("open")
        val get = intent.getStringExtra("get")
        val demo = intent.getStringExtra("demo")
        setContent { StoreApp(tick, tab, open, get, demo) }
    }

    override fun onResume() {
        super.onResume()
        tick.intValue = tick.intValue + 1
        Downloads.onResumed(applicationContext)
    }
}
