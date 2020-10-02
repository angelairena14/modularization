package com.modularization.di

import com.koinworks.app.injector.ApplicationComponent
import com.koinworks.app.injector.annotation.FeatureScope
import com.koinworks.app.injector.module.ViewModelFactoryModule
import com.modularization.base.BaseActivity
import com.modularization.ui.MainActivity
import dagger.Component

@Component(dependencies = [ApplicationComponent::class], modules = [ModularizationModule::class])
@FeatureScope
interface ModularizationComponent {
    fun inject(activity: BaseActivity)
    fun inject(activity: MainActivity)
}