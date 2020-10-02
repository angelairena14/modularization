package com.modularization.ui

import android.content.Context
import com.koinworks.app.BasePresenter
import com.koinworks.app.injector.annotation.ActivityContext
import com.koinworks.app.koinrobo.KoinRoboContract
import com.koinworks.app.network.KoinWorksApi
import com.modularization.network.NetworkService
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import javax.inject.Inject

class MainActivityPresenter @Inject constructor(@ActivityContext val context: Context, val api: NetworkService) :
    BasePresenter<MainActivityContract.View>(), MainActivityContract.Presenter {
    override fun fetchBalance() {
        disposables?.add(api.getBalanceP2p()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                mvpView?.onRetrieveBalance(it)
            }, {

            }))
    }


}