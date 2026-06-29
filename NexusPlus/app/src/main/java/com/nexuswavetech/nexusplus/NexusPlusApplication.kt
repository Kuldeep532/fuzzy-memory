package com.nexuswavetech.nexusplus

import android.app.Application
import com.nexuswavetech.nexusplus.billing.PremiumRepository
import com.nexuswavetech.nexusplus.core.SettingsRepository
import com.nexuswavetech.nexusplus.di.appModule
import com.nexuswavetech.nexusplus.model.FirstLaunchManager
import com.nexuswavetech.nexusplus.platform.PlatformContext
import com.nexuswavetech.nexusplus.remoteconfig.RemoteConfigRepository
import com.nexuswavetech.nexusplus.sound.NexusSoundManager
import org.koin.android.ext.android.get
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level

class NexusPlusApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        PlatformContext.init(this)
        startKoin {
            androidLogger(Level.ERROR)
            androidContext(this@NexusPlusApplication)
            modules(appModule)
        }
        NexusSoundManager.init(this, get<SettingsRepository>())
        get<FirstLaunchManager>().checkAndQueue()
        get<RemoteConfigRepository>().fetchAndActivate()
        get<PremiumRepository>().init()
    }
}
