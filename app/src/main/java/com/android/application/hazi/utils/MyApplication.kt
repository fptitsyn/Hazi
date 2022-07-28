package com.android.application.hazi.utils

import android.app.Application

class MyApplication : Application() {
    companion object {
        var coins = 0
        var hunger = 0
        var energy = 100
    }
}