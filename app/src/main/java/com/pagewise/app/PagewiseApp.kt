package com.pagewise.app

import android.app.Application
import com.pagewise.app.data.Prefs
import com.pagewise.app.data.Repository
import com.pagewise.app.data.db.AppDatabase
import com.pagewise.app.data.remote.RemoteSource
import com.pagewise.app.service.Notifications
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class PagewiseApp : Application() {
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val repository: Repository by lazy {
        Repository(AppDatabase.create(this), Prefs(this), RemoteSource())
    }

    override fun onCreate() {
        super.onCreate()
        Notifications.createChannels(this)
        appScope.launch { repository.warmUp() }
    }
}
