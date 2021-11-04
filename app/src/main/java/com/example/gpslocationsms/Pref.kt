package com.example.gpslocationsms

import android.content.Context

class Prefs(val context: Context) {

    val SHARED_NAME = "DriverName"
    val SHARED_USER_NAME = "username"
    val SHARED_ID_DRIVER = "IdDriver"
    val SHARED_PLACA = "placaCar"

    val storage = context.getSharedPreferences(SHARED_NAME,0)

    fun saveUser(name:String){
        storage.edit().putString(SHARED_USER_NAME, name).apply()
    }
    fun savePlaca(placa:String){
        storage.edit().putString(SHARED_PLACA, placa).apply()
    }
    fun saveIdDriver(idDriver:Int){
        storage.edit().putInt(SHARED_ID_DRIVER, idDriver).apply()
    }

    fun getUser():String{
        return storage.getString(SHARED_NAME, "")!!
    }
    fun getPlaca():String{
        return storage.getString(SHARED_PLACA, "")!!
    }
    fun getIdDriver():Int{
        return storage.getInt(SHARED_ID_DRIVER, 0)
    }


}