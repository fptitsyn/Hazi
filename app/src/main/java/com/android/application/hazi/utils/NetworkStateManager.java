package com.android.application.hazi.utils;

import android.os.Looper;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

public class NetworkStateManager {
    public static final String TAG = NetworkStateManager.class.getSimpleName();

    private static NetworkStateManager INSTANCE;
    private static final MutableLiveData<Boolean> activeNetworkStatusMLD = new MutableLiveData<>();

    private NetworkStateManager() {}

    public static synchronized NetworkStateManager getInstance() {
        if (INSTANCE == null) {
            Log.d(TAG, "getInstance() called: Creating new instance");
            INSTANCE = new NetworkStateManager();
        }
        return INSTANCE;
    }

    /**
     * Updates the active network status live-data
     */
    public void setNetworkConnectivityStatus(boolean connectivityStatus) {
        Log.d(TAG, "setNetworkConnectivityStatus() called with: connectivityStatus = [" + connectivityStatus + "]");

        if (Looper.myLooper() == Looper.getMainLooper()) {
            activeNetworkStatusMLD.setValue(connectivityStatus);
        } else {
            activeNetworkStatusMLD.postValue(connectivityStatus);
        }
    }

    /**
     * Returns the current network status
     */
    public LiveData<Boolean> getNetworkConnectivityStatus() {
        Log.d(TAG, "getNetworkConnectivityStatus() called");
        return activeNetworkStatusMLD;
    }
}
