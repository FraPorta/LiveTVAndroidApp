package com.example.livetv

import android.app.Application
import com.example.livetv.data.local.TeamDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Custom Application class that pre-warms the [TeamDatabase] as early as possible.
 *
 * [TeamDatabase.init] reads and parses `team_db.json` from assets (~400 team entries).
 * By starting it here — before any Activity or ViewModel is created — the IO work
 * overlaps with the system's own Activity/Compose inflation time. When
 * [com.example.livetv.ui.MatchViewModel] later calls `TeamDatabase.init()` it finds the
 * work already done (double-checked lock returns immediately) and can proceed straight
 * to the network fetch without the serial DB-then-network delay.
 */
class LiveTVApplication : Application() {

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        // Pre-warm the offline team / league database so MatchViewModel doesn't have
        // to wait for it before issuing the first network request.
        appScope.launch {
            TeamDatabase.init(assets)
        }
    }
}
