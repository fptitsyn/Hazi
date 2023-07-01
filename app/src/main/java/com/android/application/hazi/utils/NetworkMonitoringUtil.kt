package com.android.application.hazi.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import java.lang.Exception

class NetworkMonitoringUtil(context: Context) : ConnectivityManager.NetworkCallback() {

    private val networkRequest: NetworkRequest = NetworkRequest.Builder()
        .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
        .addTransportType(NetworkCapabilities.TRANSPORT_ETHERNET)
        .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
        .build()

    private val connectivityManager: ConnectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val networkStateManager = NetworkStateManager.getInstance()

    override fun onAvailable(network: Network) {
        super.onAvailable(network)

        networkStateManager?.setNetworkConnectivityStatus(true)
    }

    override fun onLost(network: Network) {
        super.onLost(network)

        networkStateManager?.setNetworkConnectivityStatus(false)
    }

    fun registerNetworkCallbackEvents() {
        connectivityManager.registerNetworkCallback(networkRequest, this)
    }

    fun checkNetworkState() {
        try {
            val networkInfo = connectivityManager.activeNetworkInfo
            networkStateManager?.setNetworkConnectivityStatus(networkInfo != null && networkInfo.isConnected)
        } catch (exc: Exception) {
            exc.printStackTrace()
        }
    }
}