package com.android.application.hazi.utils

import android.app.Application

class MyApplication : Application() {

//    private lateinit var networkMonitoringUtil: NetworkMonitoringUtil

    companion object {
        var coins = 0
        var hunger = 0
        var energy = 100
    }

//    override fun onCreate() {
//        super.onCreate()
//
//        networkMonitoringUtil = NetworkMonitoringUtil(applicationContext)
//        networkMonitoringUtil.checkNetworkState()
//        networkMonitoringUtil.registerNetworkCallbackEvents()
//    }
}