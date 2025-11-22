package com.troca.latroca

import android.app.Application
import android.util.Log
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.datadog.android.Datadog
import com.datadog.android.DatadogSite
import com.datadog.android.core.configuration.Configuration
import com.datadog.android.privacy.TrackingConsent
import com.datadog.android.rum.GlobalRumMonitor
import com.datadog.android.rum.Rum
import com.datadog.android.rum.RumConfiguration
import com.datadog.android.rum.tracking.ActivityViewTrackingStrategy

class LaTrocaApplication : Application(), ImageLoaderFactory {

    override fun onCreate() {
        super.onCreate()
        initializeDatadog()
    }

    private fun initializeDatadog() {
        try {
            val clientToken = "pub3c9190e3b8417338a9fc39371275ba90"
            val applicationId = "ea25a4aa-4940-4503-af62-a6b94c2d0244"
            val environmentName = if (BuildConfig.DEBUG) "development" else "production"
            val appVariantName = BuildConfig.BUILD_TYPE

            // 📊 Configuración principal de Datadog
            val configuration = Configuration.Builder(
                clientToken = clientToken,
                env = environmentName,
                variant = appVariantName
            )
                .useSite(DatadogSite.US5)
                .build() // ⬅️ Eliminamos trackCrashes() de aquí

            // 🚀 Inicializar Datadog (sin el parámetro credentials)
            Datadog.initialize(
                context = this,
                configuration = configuration, // ⬅️ Se llama 'configuration', no 'credentials'
                trackingConsent = TrackingConsent.GRANTED
            )

            // 📱 Configuración de RUM (Real User Monitoring)
            val rumConfiguration = RumConfiguration.Builder(applicationId)
                .trackUserInteractions()
                .trackLongTasks(100L)
                .useViewTrackingStrategy(ActivityViewTrackingStrategy(true))
                .setSessionSampleRate(100f)
                .setTelemetrySampleRate(100f)
                .trackNonFatalAnrs(true) // ⬅️ Tracking de ANRs
                .build()

            // ✅ Habilitar RUM
            Rum.enable(rumConfiguration)

            // 📊 Agregar atributos globales
            GlobalRumMonitor.get().apply {
                addAttribute("app_version", BuildConfig.VERSION_NAME)
                addAttribute("app_version_code", BuildConfig.VERSION_CODE)
                addAttribute("user_type", "standard")
            }

            Log.d("Datadog", "✅ Datadog ${BuildConfig.VERSION_NAME} inicializado correctamente")

        } catch (e: Exception) {
            Log.e("Datadog", "❌ Error inicializando Datadog: ${e.message}", e)
        }
    }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.25)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizeBytes(50 * 1024 * 1024)
                    .build()
            }
            .respectCacheHeaders(false)
            .build()
    }
}