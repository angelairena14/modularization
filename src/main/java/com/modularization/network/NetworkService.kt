package com.modularization.network

import com.google.gson.Gson
import com.koinworks.app.model.response.BalanceP2pResponse
import io.reactivex.Flowable
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import javax.inject.Inject

interface NetworkService {
    @GET("v1/koinp2p/get_balance")
    fun getBalanceP2p(): Flowable<BalanceP2pResponse>

    class Creator {
        @Inject
        fun modularizationApi(url: String, httpClient: OkHttpClient, gson: Gson): NetworkService {
            val retrofit = Retrofit
                .Builder()
                .baseUrl(url)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .client(httpClient)
                .build()

            return retrofit.create(NetworkService::class.java)
        }
    }
}