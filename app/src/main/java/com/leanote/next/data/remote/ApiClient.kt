package com.leanote.next.data.remote

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ApiClient @Inject constructor() {

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        explicitNulls = false
    }

    private val okHttp: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .apply {
            if (com.leanote.next.BuildConfig.DEBUG) {
                addInterceptor(HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.BASIC
                })
            }
        }
        .build()

    private val cache = ConcurrentHashMap<String, LeanoteApi>()

    fun api(host: String): LeanoteApi {
        val base = normalizeHost(host)
        return cache.getOrPut(base) {
            Retrofit.Builder()
                .baseUrl("$base/api/")
                .client(okHttp)
                .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
                .build()
                .create(LeanoteApi::class.java)
        }
    }

    fun imageUrl(host: String, fileId: String, token: String? = null): String {
        val base = normalizeHost(host)
        val suffix = if (token.isNullOrBlank()) "" else "&token=$token"
        return "$base/api/file/getImage?fileId=$fileId$suffix"
    }

    companion object {
        const val DEFAULT_HOST = "https://leanote.com"

        /** 接受 leanote.com、https://xxx、xxx/api 等写法，统一成不带结尾斜杠的根地址。 */
        fun normalizeHost(raw: String): String {
            var host = raw.trim()
            if (host.isEmpty()) host = DEFAULT_HOST
            if (!host.startsWith("http://") && !host.startsWith("https://")) {
                host = "https://$host"
            }
            host = host.trimEnd('/')
            if (host.endsWith("/api")) host = host.removeSuffix("/api")
            return host
        }
    }
}
