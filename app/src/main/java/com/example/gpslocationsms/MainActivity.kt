package com.example.gpslocationsms


import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import android.widget.TextView
import kotlinx.coroutines.*
import java.util.*
import androidx.core.app.ActivityCompat
import android.content.pm.PackageManager
import android.location.Location
import com.google.android.gms.location.*
import java.io.IOException


class MainActivity : AppCompatActivity() {

    private lateinit var locationCallback: LocationCallback
    private lateinit var locationRequest: LocationRequest
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var timer: Timer
    private var socketUDP = arrayListOf<SocketUDP>()
    private var started: Boolean = false

    private val coarseLocationPermission = PermissionLocationRequester(
        this,
        Manifest.permission.ACCESS_FINE_LOCATION,
        onRationale = {toastLong("Para obtener la ubicaíon es necesario el permiso de Location")},
        onDenied = {openAppSettings()
            toastLong("Dirijase a permisos, en location y active la localización para obtener su ubicación")}
    )

    private val coarseLocationPermissionBackground = PermissionLocationRequester(
        this,
        Manifest.permission.ACCESS_BACKGROUND_LOCATION,
        onRationale = {toastLong("Para usar la aplicación active los permisos de segundo plano")},
        onDenied = {openAppSettings()
            toastLong("Dirijase a permisos, en location y active la localización en segundo plano para obtener su ubicación")}
    )

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId){
            R.id.login_menu_back -> {
                val ventanaMenu = Intent(applicationContext, LoginActivity::class.java)
                startActivity(ventanaMenu)
            }
        }
        return super.onOptionsItemSelected(item)
    }

    @SuppressLint("MissingPermission")
    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val getLocation = findViewById<Button>(R.id.buttonObtainLocation)
        val endGettingLocation = findViewById<Button>(R.id.buttonEndLocation)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        getLocationUpdates()

        GlobalScope.launch(Dispatchers.Main) {
            withContext(Dispatchers.Main){
            }
        }

        getLocation.setOnClickListener{
            coarseLocationPermission.runWithPermission{
                coarseLocationPermissionBackground.runWithPermission {
                    try {
                        val host = arrayListOf(
                            "34.211.44.164",
                        )

                        host.forEach { socketUDP.add(SocketUDP(it, 9000)) }
                        socketUDP.forEach{it.start()}
                        timer = Timer()
                        val dierickTimer = initLocation()
                        timer.scheduleAtFixedRate(dierickTimer, 1000, 5000)
                        started = true
                    }catch (e: IOException){
                        e.printStackTrace()
                    }catch (e: NumberFormatException){
                        e.printStackTrace()
                        toastShort("Debe Introducir un número puerto")
                    }
                }
            }
            socketUDP = arrayListOf()
        }

        endGettingLocation.setOnClickListener{
            socketUDP = arrayListOf()
            if(started){
                timer.cancel()
                socketUDP.forEach{it.close()}
                started = false
            }
        }
    }

    override fun onResume() {
        super.onResume()
        startLocationUpdates()
    }

    private fun initLocation():TimerTask{
        return object: TimerTask(){
            @SuppressLint("MissingPermission")
            override fun run() {
                coarseLocationPermission.runWithPermission{
                    coarseLocationPermissionBackground.runWithPermission {
                        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                            if (location != null) {
                                findViewById<TextView>(R.id.textViewLocation).text = ("${location.latitude}, ${location.longitude}")
                                toastShort("Ubicación enviada")
                                socketUDP.forEach { it.send("${location.latitude},${location.longitude}, ${location.time}") }
                            } else {
                                toastShort("Ubicación desconocida, no se ha enviado la ubicación")
                            }
                        }
                    }
                }
            }
        }
    }

    private fun startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            return
        }
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null)
    }

    private fun getLocationUpdates() {
        locationRequest = LocationRequest.create().apply {
            interval = 3000
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            maxWaitTime= 100
            smallestDisplacement = 0.5f
        }
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult?) {
                locationResult ?: return
            }
        }
    }

}







