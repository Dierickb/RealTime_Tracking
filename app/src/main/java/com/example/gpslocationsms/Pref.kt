package com.example.gpslocationsms

import android.content.Context

class Prefs(val context: Context) {

    val SHARED_NAME = "DriverName"
    val SHARED_USER_NAME = "username"
    val SHARED_ID_DRIVER = "IdDriver"
    val SHARED_PLACA = "placaCar"

    val storage = context.getSharedPreferences(SHARED_NAME,0)

    fun savePlaca(placa:String){
        storage.edit().putString(SHARED_PLACA, placa).apply()
    }
    fun saveIdDriver(idDriver:String){
        storage.edit().putString(SHARED_ID_DRIVER, idDriver).apply()
    }
    fun saveDrivingName(drivingName:String){
        storage.edit().putString(SHARED_USER_NAME, drivingName).apply()
    }

    fun getPlaca():String{
        return storage.getString(SHARED_PLACA, "")!!
    }
    fun getIdDriver():String{
        return storage.getString(SHARED_ID_DRIVER, "")!!
    }
    fun getUserName():String{
        return storage.getString(SHARED_USER_NAME, "")!!
    }


}