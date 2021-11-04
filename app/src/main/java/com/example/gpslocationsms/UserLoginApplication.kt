package com.example.gpslocationsms

import android.app.Application

class UserLoginApplication: Application() {

    companion object{
        lateinit var prefs: Prefs
    }

    override fun onCreate() {
        super.onCreate()
        prefs = Prefs(applicationContext)
    }
}