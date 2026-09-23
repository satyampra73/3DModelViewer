package com.app.a3dmodelviewer.engine

import android.content.Context
import com.google.android.filament.Engine
import com.google.android.filament.Filament
import com.google.android.filament.gltfio.Gltfio
import com.google.android.filament.gltfio.UbershaderProvider
import com.google.android.filament.utils.Utils

object FilamentManager {

    private var isInitialized = false
    private var _engine: Engine? = null
    private var _materialProvider: UbershaderProvider? = null

    val engine: Engine
        get() = checkNotNull(_engine) { "FilamentManager has not been initialized. Call init(context) first." }

    val materialProvider: UbershaderProvider
        get() = checkNotNull(_materialProvider) { "FilamentManager has not been initialized. Call init(context) first." }

    fun init(context: Context) {
        if (isInitialized) return

        // Initialize native JNI libraries
        Utils.init()
        Filament.init()
        Gltfio.init()

        // Create the single shared engine instance
        val eng = Engine.create()
        _engine = eng
        _materialProvider = UbershaderProvider(eng)
        isInitialized = true
    }

    fun destroy() {
        if (!isInitialized) return

        _materialProvider?.destroy()
        _materialProvider = null

        _engine?.destroy()
        _engine = null

        isInitialized = false
    }
}
