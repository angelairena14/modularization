package com.modularization.ui

import com.koinworks.app.BaseInterface
import com.koinworks.app.model.response.BalanceP2pResponse

interface MainActivityContract {
    interface View: BaseInterface {
        fun onRetrieveBalance(res : BalanceP2pResponse)
    }

    interface Presenter{
        fun fetchBalance()
    }
}