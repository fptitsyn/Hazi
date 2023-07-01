package com.android.application.hazi.utils

import android.os.Looper
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData


class NetworkStateManagerKT {

//    companion object {
//        var INSTANCE: NetworkStateManager? = null
//        get() {
//            if (field == null) {
//                field = NetworkStateManager()
//            }
//            return field
//        }
//        val activeNetworkStatus: MutableLiveData<Boolean> = MutableLiveData()
//    }
//
//    fun setNetworkConnectivityStatus(connectivityStatus: Boolean) {
//        if (Looper.myLooper() == Looper.getMainLooper()) {
//            activeNetworkStatus.value = connectivityStatus
//        } else {
//            activeNetworkStatus.postValue(connectivityStatus)
//        }
//    }

    private var INSTANCE: NetworkStateManagerKT? = null
    private val activeNetworkStatusMLD = MutableLiveData<Boolean>()

    fun getInstance(): NetworkStateManagerKT? {
        if (INSTANCE == null) {
            INSTANCE = NetworkStateManagerKT()
        }
        return INSTANCE
    }

    /**
     * Updates the active network status live-data
     */
    fun setNetworkConnectivityStatus(connectivityStatus: Boolean) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            activeNetworkStatusMLD.setValue(connectivityStatus)
        } else {
            activeNetworkStatusMLD.postValue(connectivityStatus)
        }
    }

    /**
     * Returns the current network status
     */
    fun getNetworkConnectivityStatus(): LiveData<Boolean>? {
        return activeNetworkStatusMLD
    }

}

