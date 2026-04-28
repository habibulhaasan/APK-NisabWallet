package com.hasan.nisabwallet

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.PersistentCacheSettings
import com.hasan.nisabwallet.BuildConfig
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class NisabWalletApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override fun onCreate() {
        super.onCreate()
        initFirestore()
    }

    private fun initFirestore() {
        // FIX: Use the new cache API (the old setCacheSizeBytes is deprecated in newer SDKs)
        val settings = FirebaseFirestoreSettings.Builder()
            .setLocalCacheSettings(
                PersistentCacheSettings.newBuilder()
                    .setSizeBytes(FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED)
                    .build()
            )
            .build()
        FirebaseFirestore.getInstance().firestoreSettings = settings

        // FIX: Enable network logging in debug builds to catch silent failures
        if (BuildConfig.DEBUG) {
            FirebaseFirestore.setLoggingEnabled(true)
        }
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}
