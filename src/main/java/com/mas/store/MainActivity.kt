package com.mas.store

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.mutableIntStateOf

class MainActivity : ComponentActivity() {
    private val tick = mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val tab = intent.getStringExtra("tab")
        val open = intent.getStringExtra("open")
        val get = intent.getStringExtra("get")
        setContent { StoreApp(tick, tab, open, get) }
    }

    override fun onResume() {
        super.onResume()
        tick.intValue = tick.intValue + 1
    }
}
