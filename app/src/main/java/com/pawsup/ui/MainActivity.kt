package com.pawsup.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.pawsup.data.UserPreferences
import com.pawsup.monitoring.MonitoringService
import com.pawsup.onboarding.OnboardingActivity
import com.pawsup.settings.SettingsActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var prefs: UserPreferences
    @Inject lateinit var monitoring: com.pawsup.monitoring.MonitoringRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycleScope.launch {
            val onboardingDone = prefs.onboardingCompleted.first()
            if (onboardingDone && prefs.snapshotMonitorMeEnabled()) {
                com.pawsup.monitoring.MonitoringAccess.start(this@MainActivity, monitoring)
            }
            val target = if (onboardingDone) SettingsActivity::class.java
                         else OnboardingActivity::class.java
            startActivity(Intent(this@MainActivity, target))
            finish()
        }
    }
}
