package com.modularization.di

import android.app.Activity
import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.Gson
import com.koinworks.app.BuildConfig
import com.koinworks.app.injector.annotation.ActivityContext
import com.koinworks.app.injector.annotation.FeatureScope
import com.modularization.network.NetworkService
import dagger.Module
import dagger.Provides
import okhttp3.OkHttpClient

@Module
class ModularizationModule(private val activity: AppCompatActivity) {

    @Provides
    @FeatureScope
    fun provideActivity(): Activity = activity

    @Provides
    @ActivityContext
    @FeatureScope
    fun provideContext(): Context = activity

    @Provides
    @FeatureScope
    fun provideModulApi(okHttpClient: OkHttpClient, gson: Gson): NetworkService {
        return NetworkService.Creator().modularizationApi(BuildConfig.ENDPOINT_API, okHttpClient, gson)
    }
}