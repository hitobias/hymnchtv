package org.cog.hymnchtv.update

import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

/** Shared OkHttp client for update traffic; built lazily so app start-up never pays for it. */
object UpdateHttp {
    private val CLIENT: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .callTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    @JvmStatic
    fun client(): OkHttpClient = CLIENT
}
