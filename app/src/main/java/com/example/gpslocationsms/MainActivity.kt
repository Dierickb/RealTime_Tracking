package com.example.gpslocationsms


import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import android.widget.TextView
import java.util.*
import androidx.core.app.ActivityCompat
import android.content.pm.PackageManager
import android.location.Location
import android.os.Handler
import android.os.Looper
import androidx.core.content.ContextCompat
import com.example.gpslocationsms.UserLoginApplication.Companion.prefs
import com.github.pires.obd.commands.SpeedCommand
import com.github.pires.obd.commands.engine.RPMCommand
import com.google.android.gms.location.*
import java.io.IOException
import com.github.pires.obd.commands.protocol.EchoOffCommand
import com.github.pires.obd.commands.protocol.LineFeedOffCommand
import com.github.pires.obd.commands.protocol.SelectProtocolCommand
import com.github.pires.obd.commands.protocol.TimeoutCommand
import com.github.pires.obd.commands.temperature.AmbientAirTemperatureCommand
import com.github.pires.obd.enums.ObdProtocols

class MainActivity : AppCompatActivity() {

    private var bluetoothIcon = false
    private lateinit var locationCallback: LocationCallback
    private lateinit var locationRequest: LocationRequest
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var timer: Timer
    private var socketUDP = arrayListOf<SocketUDP>()
    private var started: Boolean = false
    private val REQUEST_ENABLE_BT = 1234
    private val bluetooth:Bluetooth = Bluetooth(handler = Handler(Looper.getMainLooper()))
    private lateinit var socketBt: BluetoothSocket
    private lateinit var threadBT: Bluetooth.ConnectedThread
    var conected = false

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

    fun goToLogin(){
        startActivity(Intent(applicationContext,
            LoginActivity::class.java))
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)

        setBluetoothIcon(menu.findItem(R.id.bluethoot_menu)!!)

        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId){
            R.id.login_menu_back -> {
                val ventanaMenu = Intent(applicationContext, LoginActivity::class.java)
                startActivity(ventanaMenu)
            }
        }

        when(item.itemId){
            R.id.bluethoot_menu -> {
                bluetoothIcon = !bluetoothIcon
                setBluetoothIcon(item)
                startBluetooth()
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

        getLocation.setOnClickListener{
            coarseLocationPermission.runWithPermission{
                coarseLocationPermissionBackground.runWithPermission {

                    if( ( prefs.getIdDriver()==0 ||
                                prefs.getPlaca().isEmpty())){
                        goToLogin()
                        toastLong("Es necesario que se registre")

                    }else{

                        try {

                            val host = arrayListOf(
                                "34.209.26.69",
                                "186.114.164.166",
                                "190.84.119.89",
                                "54.189.98.176",
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
                        val rpmCommand = RPMCommand()
                        val speedCommand = SpeedCommand()
                        val tvRpm = findViewById<TextView>(R.id.tvRpm)

                        if(conected){
                            try {
                                rpmCommand.run(socketBt.inputStream, socketBt.outputStream)
                                speedCommand.run(socketBt.inputStream, socketBt.outputStream)
                                ("RPM: " +rpmCommand.formattedResult).also{tvRpm.text = it}
                            }catch(e: IOException){

                            }
                        }

                        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                            if (location != null ) {
                                if (conected){
                                    socketUDP.forEach { it.send("${"%.7f".format(location.latitude)},${"%.7f".format(location.longitude)},${location.time},${rpmCommand.rpm}") }
                                }else{
                                    socketUDP.forEach { it.send("${"%.7f".format(location.latitude)},${"%.7f".format(location.longitude)},${location.time},0") }
                                }
                                findViewById<TextView>(R.id.textViewLocation).text = ("${location.latitude}, ${location.longitude}")
                                toastShort("Ubicación enviada")

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

    private fun startBluetooth(){
        if (!conected){
            var deviceHardwareAddress:String? = null
            val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
            if (bluetoothAdapter?.isEnabled == false) {
                val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
            }
            val pairedDevices: Set<BluetoothDevice>? = bluetoothAdapter?.bondedDevices
            pairedDevices?.forEach { device ->
                val deviceName = device.name
                if (deviceName == "OBDII"){
                    println("$deviceName ")
                    deviceHardwareAddress = device.address // MAC address
                }
            }
            if(deviceHardwareAddress != null) {
                val bluetoothDevice =  bluetoothAdapter?.getRemoteDevice(deviceHardwareAddress)
                if (bluetoothDevice != null) {
                    val thread = Thread{
                        try {
                            socketBt = bluetooth.connect(bluetoothDevice)!!
                            threadBT = bluetooth.ConnectedThread(socketBt)

                            conected = true
                            try {
                                EchoOffCommand().run(socketBt.inputStream, socketBt.outputStream)
                                LineFeedOffCommand().run(socketBt.inputStream, socketBt.outputStream)
                                TimeoutCommand(125).run(socketBt.inputStream, socketBt.outputStream)
                                SelectProtocolCommand(ObdProtocols.AUTO).run(socketBt.inputStream, socketBt.outputStream)
                                AmbientAirTemperatureCommand().run(socketBt.inputStream, socketBt.outputStream)
                            } catch (e: Exception) {
                                // handle errors
                            }
                        }catch (e: IOException) {
                            e.printStackTrace()
                        }
                    }
                    thread.start()
                }
            }else{
                toastShort("Conecte el dispositivo manualmente desde configuraciones")
            }

        }else{
            if (::threadBT.isInitialized){
                val res = threadBT.cancel()
                if (res) {
                    conected = false
                }

            }
        }
    }

    private fun setBluetoothIcon(menuItem: MenuItem){
        val id = if(bluetoothIcon){
            R.drawable.ic_baseline_bluetooth_connected_24
        }else {R.drawable.ic_baseline_bluetooth_24}

        menuItem.icon = ContextCompat.getDrawable(this, id )
    }

}







