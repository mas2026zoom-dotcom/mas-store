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
        val tab = intent.getStringExtra("tab")
        val open = intent.getStringExtra("open")
        val get = intent.getStringExtra("get")
        val demo = intent.getStringExtra("demo")
        if (intent.getStringExtra("ob") == "skip") getSharedPreferences("mas", MODE_PRIVATE).edit().putBoolean("onboarded", true).apply()
        setContent { StoreApp(tick, tab, open, get, demo) }
    }

    override fun onResume() {
        super.onResume()
        tick.intValue = tick.intValue + 1
        Downloads.onResumed(applicationContext)
    }
}
