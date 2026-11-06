package com.example.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.MainActivity
import com.example.focuslock.ui.screens.blocking.BlockingScreen
import com.example.ui.theme.FocusLockTheme

class BlockingActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val packageName = intent.getStringExtra(EXTRA_PACKAGE_NAME) ?: "com.example"
        val endTime = intent.getLongExtra(EXTRA_END_TIME, 0L)
        val isStrictMode = intent.getBooleanExtra(EXTRA_STRICT_MODE, false)

        setContent {
            FocusLockTheme {
                BackHandler {
                    returnToHomeScreen()
                }

                BlockingScreen(
                    blockedPackageName = packageName,
                    endTimeMillis = endTime,
                    isStrictMode = isStrictMode,
                    onReturnHome = { returnToHomeScreen() },
                    onOpenFocusLock = {
                        val mainIntent = Intent(this@BlockingActivity, MainActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                        }
                        startActivity(mainIntent)
                        finish()
                    }
                )
            }
        }
    }

    private fun returnToHomeScreen() {
        val homeIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(homeIntent)
        finish()
    }

    companion object {
        const val EXTRA_PACKAGE_NAME = "extra_package_name"
        const val EXTRA_END_TIME = "extra_end_time"
        const val EXTRA_STRICT_MODE = "extra_strict_mode"
        const val EXTRA_SESSION_ID = "extra_session_id"
    }
}
