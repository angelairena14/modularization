package com.modularization.ui

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.google.gson.Gson
import com.koinworks.app.model.response.BalanceP2pResponse
import com.modularization.R
import com.modularization.base.BaseActivity
import javax.inject.Inject

class MainActivity : BaseActivity() ,MainActivityContract.View{
    override fun onRetrieveBalance(res: BalanceP2pResponse) {
        Log.i("balance_is",Gson().toJson(res))
    }

    @Inject
    lateinit var presenter: MainActivityPresenter
    override fun getLayoutResource(): Int {
        return R.layout.activity_modularization
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        getActivityComponent()?.inject(this)
        presenter.attachView(this)
        presenter.fetchBalance()
    }
}
