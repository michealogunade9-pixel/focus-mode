package com.example.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Compatibility BroadcastReceiver under com.example.service delegating to
 * the primary FocusLock BootReceiver.
 */
class BootReceiver : BroadcastReceiver() {
    private val delegate = com.example.focuslock.service.BootReceiver()

    override fun onReceive(context: Context, intent: Intent) {
        delegate.onReceive(context, intent)
    }
}
