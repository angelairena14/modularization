package com.modularization.base

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.Window
import android.view.inputmethod.InputMethodManager
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import butterknife.BindView
import butterknife.ButterKnife
import com.koinworks.app.BaseApplication
import com.koinworks.app.injector.RxBus
import com.koinworks.app.koinp2p.dialog.ReqTimeoutDialog
import com.koinworks.app.model.NotificationData
import com.koinworks.app.model.notification.NotificationCounter
import com.koinworks.app.R as SuperR
import com.koinworks.app.notification.GetNotificationContract
import com.koinworks.app.notification.GetNotificationPresenter
import com.koinworks.app.notification.NotificationActivity
import com.koinworks.app.util.Constants
import com.koinworks.app.util.MyContextWrapper
import com.koinworks.app.util.PreferencesUtil
import com.koinworks.app.util.bottomsheet.DashboardSheetDialog
import com.koinworks.app.util.bottomsheet.NoInternetSheetDialog
import com.koinworks.app.util.bottomsheet.ServerBusySheetDialog
import com.modularization.R
import com.modularization.di.DaggerModularizationComponent
import com.modularization.di.ModularizationComponent
import com.modularization.di.ModularizationModule
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import java.util.*
import javax.inject.Inject

abstract class BaseActivity: AppCompatActivity(), GetNotificationContract.View, DashboardSheetDialog.BottomSheetListener {
    private var modularizationComponent: ModularizationComponent? = null

    lateinit var context: Context
    private lateinit var noInternetSheetDialog: NoInternetSheetDialog

    private var listenNotify: Disposable? = null
    private var listenCounterNotify: Disposable? = null
    private var disposable: Disposable? = null

    private var isPause = true
    private var isServerBusyOpened = true
    private var isServerTimeOut = true

    private var alertDialog: AlertDialog? = null

    @Inject
    lateinit var notifyPresenter: GetNotificationPresenter

    var bind: ViewDataBinding? = null

    protected abstract fun getLayoutResource(): Int
    override fun onButtonClicked(number: Int) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        getActivityComponent()?.inject(this)
        notifyPresenter.attachView(this)
        setContentView(getLayoutResource())
        try {
            bind = null
            bind = DataBindingUtil.setContentView(this, getLayoutResource())
        } catch (e: IllegalStateException) {
            e.printStackTrace()
        }
        ButterKnife.bind(this)
        context = this

        /*setLanguage()
        findViewById<View?>(R.id.btn_language)?.setOnClickListener {
            val lang: String = PreferencesUtil.getLanguage(context)
            PreferencesUtil.setLanguage(context, if (lang == "en") "id" else "en")
            val intent: Intent = intent
            finish()
            startActivity(intent)
            overridePendingTransition(SuperR.anim.fade_in, SuperR.anim.fade_out)
        }*/

        setupNotification()
        findViewById<View?>(R.id.img_notification)?.setOnClickListener {
            showActivity(Intent(context, NotificationActivity::class.java))
        }

        val bottomSheet = DashboardSheetDialog()
        findViewById<View?>(R.id.img_menu)?.setOnClickListener {
            bottomSheet.show(supportFragmentManager, DashboardSheetDialog::class.java.name)
        }
        setupNoInternetDialog()
    }

    inline fun <reified T> getBind(): T? {
        return this.bind as T?
    }

    fun getActivityComponent(): ModularizationComponent? {
        if (modularizationComponent == null) {
            modularizationComponent = DaggerModularizationComponent.builder()
                .applicationComponent(BaseApplication.get(this).getApplicationComponent())
                .modularizationModule(ModularizationModule(this))
                .build()
        }
        return modularizationComponent
    }

    fun showArrowSaldoHeader() {
        findViewById<View?>(R.id.ll_saldo)?.let {
            findViewById<View?>(R.id.arrow_cash)?.visibility = View.VISIBLE
            findViewById<View?>(R.id.arrow_koin)?.visibility = View.VISIBLE
        } ?: run {
            //            throw RuntimeException("R.layout.partial_header_saldo not included to main layout")
        }
    }

    fun changeLanguage(activity: Activity, lang: String){
        val locale = Locale(lang)
        Locale.setDefault(locale)
        val configuration = activity.baseContext.resources.configuration
        configuration.setLocale(locale)
        activity.baseContext.resources.updateConfiguration(configuration, activity.baseContext.resources.displayMetrics)
        activity.baseContext.applicationContext.resources.updateConfiguration(configuration, activity.baseContext.resources.displayMetrics)
    }

    fun configureToolbar(
        withBackArrow: Boolean,
        toolbar: Toolbar?,
        listener: View.OnClickListener?
    ) {
        if (toolbar != null) {
            setSupportActionBar(toolbar)
            val supportActionBar = supportActionBar
                ?: throw RuntimeException("Unable to set support action bar")

            if (withBackArrow) {
                toolbar.setNavigationOnClickListener(listener)
                supportActionBar.setDisplayHomeAsUpEnabled(true)
                supportActionBar.setDisplayShowHomeEnabled(true)
            }
            supportActionBar.setDisplayShowTitleEnabled(false)
            supportActionBar.setHomeButtonEnabled(true)
        }
    }

    fun showActivity(intent: Intent) {
        startActivity(intent)
        overridePendingTransition(
            SuperR.anim.pull_in_right,
            SuperR.anim.push_out_left
        )
    }

    fun showActivity(intent: Intent, request_code: Int) {
        startActivityForResult(intent, request_code)
        overridePendingTransition(
            SuperR.anim.pull_in_right,
            SuperR.anim.push_out_left
        )
    }

    fun showActivityFinish(intent: Intent) {
        showActivity(intent)
        finish()
    }

    fun showActivity(packageContext: Context, cls: Class<*>) {
        val intent = getIntent(packageContext, cls)
        showActivity(intent)
    }

    fun showActivityClear(packageContext: Context, cls: Class<*>) {
        val intent = getIntent(packageContext, cls)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
        showActivity(intent)
    }

    fun showActivityFinish(packageContext: Context, cls: Class<*>) {
        showActivity(packageContext, cls)
        finish()
    }

    fun finishActivity() {
        finish()
        overridePendingTransition(SuperR.anim.pull_in_left, SuperR.anim.push_out_right)
    }

    fun showLoading() {
        if (!isPause) {
            alertDialog = AlertDialog.Builder(context).create()
            alertDialog?.requestWindowFeature(Window.FEATURE_NO_TITLE)
            alertDialog?.setView(layoutInflater.inflate(SuperR.layout.partial_loading_kw, null))
            alertDialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)
            //alertLoading?.window?.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            val lp = alertDialog?.window?.attributes
            lp?.dimAmount = 0.0f
            alertDialog?.window?.attributes = lp
            alertDialog?.show()
        }
    }

    fun dismissLoading() {
        alertDialog?.dismiss()
    }

    fun showToastMessage(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    fun hideSoftKeyboard(context: Context) {
        val imm = context.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        var view = (context as AppCompatActivity).currentFocus
        if (view == null) {
            view = View(context)
        }
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }

    fun getLocaleLanguage(): String {
        return Locale.getDefault().language
    }

    protected fun hideKeyboard(view: View?) {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        if (view != null) {
            imm.hideSoftInputFromWindow(view.windowToken, 0)
        }
    }

    private fun setupNotification() {
        this.listenNotify = RxBus
            .listen(NotificationData::class.java)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeOn(Schedulers.io())
            .subscribe({
                notifyPresenter.getCounter(true)
            },{})

        this.listenCounterNotify = RxBus
            .listen(NotificationCounter::class.java)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeOn(Schedulers.io())
            .subscribe({
                setCounter(it.UnreadNotification)
            },{})
    }

    private fun setupNoInternetDialog() {
        noInternetSheetDialog = NoInternetSheetDialog()
        noInternetSheetDialog.mInteface = object : NoInternetSheetDialog.Interface {
            override fun onTryAgain() {
                noInternetSheetDialog.dismiss()
                recreate()
            }
        }

        if (!isNetworkAvailable(this)) {
            if (!noInternetSheetDialog.isVisible) {
                noInternetSheetDialog.show(supportFragmentManager, NoInternetSheetDialog::class.java.name)
            }
        }
    }

    private fun isOnline(): Boolean {
        val cm =
            context.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork = cm.activeNetworkInfo
        return activeNetwork != null && activeNetwork.isConnected
    }

    private fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val nw      = connectivityManager.activeNetwork ?: return false
            val actNw = connectivityManager.getNetworkCapabilities(nw) ?: return false
            return actNw.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                    actNw.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                    actNw.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) ||
                    actNw.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH)
        } else {
            return isOnline()
        }
    }

    private fun getIntent(packageContext: Context, cls: Class<*>): Intent {
        return Intent(packageContext, cls)
    }

//    private fun setLanguage() {
//        val tvEn = findViewById<TextView?>(R.id.tv_en)
//        val tvId = findViewById<TextView?>(R.id.tv_id)
//        when (PreferencesUtil.getLanguage(context)) {
//            Constants.LocaleLanguage.LOCAL_ID -> {
//                tvEn?.let { changeColor(tvEn, false) }
//                tvId?.let { changeColor(tvId, true) }
//            }
//            Constants.LocaleLanguage.EN -> {
//                tvId?.let { changeColor(tvId, false) }
//                tvEn?.let { changeColor(tvEn, true) }
//            }
//        }
//    }

    private fun changeColor(it: TextView, colored: Boolean) {
        val pad: Int = resources.getDimension(SuperR.dimen.dp10).toInt()
        it.setBackgroundResource(if (colored) SuperR.color.bluePrimary else android.R.color.transparent)
        it.setPadding(pad, 0, pad, 0)
        it.setTextColor(
            ContextCompat.getColor(
                context,
                if (colored) SuperR.color.whitePure else SuperR.color.greySecondary
            )
        )
    }

    override fun attachBaseContext(newBase: Context?) {
        super.attachBaseContext(MyContextWrapper.wrap(newBase))
    }

    override fun setError(message: String) {

    }

    override fun setMessage(message: String) {

    }

    override fun setCounter(value: Int) {
        /*if (::indicator.isInitialized) {
            indicator.visibility = if (value > 0) View.VISIBLE else View.GONE
        }*/
        findViewById<View?>(com.koinworks.app.R.id.indicator)?.let { v ->
            if (value > 0) {
                v.visibility = View.VISIBLE
            } else {
                v.visibility = View.GONE
            }
        }
    }

    override fun onResume() {
        super.onResume()
        notifyPresenter.getCounter(false)
        isPause = false
        disposable = RxBus.listen(ServerBusySheetDialog::class.java)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeOn(Schedulers.io())
            .subscribe({
                if (isServerBusyOpened) {
                    it.setOnRetry {
                        finish()
                        startActivity(intent)
                    }.setOnResume {
                        isServerBusyOpened = false
                    }.setOnDestroy {
                        isServerBusyOpened = true
                    }.show(supportFragmentManager, ServerBusySheetDialog::class.java.simpleName)
                }
            },{})

        disposable = RxBus.listen(ReqTimeoutDialog::class.java)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeOn(Schedulers.io())
            .subscribe({
                if (isServerTimeOut && !it.isAdded) {
                    isServerTimeOut = false
                    it.setOnRetry {
                        finish()
                        startActivity(intent)
                    }.setOnResume {
                        isServerTimeOut = false
                    }.setOnDestroy {
                        isServerTimeOut = true
                    }.show(supportFragmentManager, ReqTimeoutDialog::class.java.simpleName)
                }
            },{})

    }

    override fun onPause() {
        isPause = true
        disposable?.dispose()
        super.onPause()
    }

    override fun onStop() {
        isPause = true
        super.onStop()
    }

    override fun onDestroy() {
        this.listenNotify?.dispose()
        this.listenCounterNotify?.dispose()
        this.disposable?.dispose()
        this.notifyPresenter.detachView()
        super.onDestroy()
    }
}