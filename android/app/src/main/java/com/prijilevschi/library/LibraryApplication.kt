package com.prijilevschi.library

import android.app.Application
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import com.prijilevschi.library.data.LibraryRepository
import com.prijilevschi.library.data.SettingsRepository

/** Manual dependency container. */
class AppContainer(application: Application) {
    val settingsRepository = SettingsRepository(application)
    val libraryRepository = LibraryRepository(settingsRepository, LibraryRepository.defaultHttpClient())
}

class LibraryApplication : Application(), SingletonImageLoader.Factory {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }

    override fun newImageLoader(context: PlatformContext): ImageLoader =
        ImageLoader.Builder(context)
            .components { add(OkHttpNetworkFetcherFactory(callFactory = { container.libraryRepository.httpClient })) }
            .build()
}
