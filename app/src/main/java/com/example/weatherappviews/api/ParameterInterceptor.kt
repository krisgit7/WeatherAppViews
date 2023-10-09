package com.example.weatherappviews.api

import okhttp3.Interceptor
import okhttp3.Response

class ParameterInterceptor : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val url = chain.request().url().newBuilder()
            .addQueryParameter("appid", "ef183d497d13948cfa507e4e98f3efc9")
            .build()

        val request = chain.request().newBuilder()
            .url(url)
            .build()

        return chain.proceed(request)
    }
}