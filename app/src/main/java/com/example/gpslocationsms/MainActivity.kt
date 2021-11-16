package com.example.gpslocationsms


import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.gpslocationsms.UserLoginApplication.Companion.prefs
import com.github.pires.obd.commands.SpeedCommand
import com.github.pires.obd.commands.engine.RPMCommand
import com.github.pires.obd.commands.protocol.EchoOffCommand
import com.github.pires.obd.commands.protocol.LineFeedOffCommand
import com.github.pires.obd.commands.protocol.SelectProtocolCommand
import com.github.pires.obd.commands.protocol.TimeoutCommand
import com.github.pires.obd.commands.temperature.AmbientAirTemperatureCommand
import com.github.pires.obd.enums.ObdProtocols
import com.google.android.gms.location.*
import java.io.IOException
import java.util.*


class MainActivity : AppCompatActivity() {

    var bluetoothIcon: Boolean = true
    private lateinit var locationCallback: LocationCallback
    private lateinit var locationRequest: LocationRequest
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var timer: Timer
    private var socketUDP = arrayListOf<SocketUDP>()
    private var started: Boolean = false
    var bluetoothsending: Boolean = false
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

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
        bluetoothIcon = bluetoothAdapter?.isEnabled != false

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

                if(conected){
                    toastShort("OBDII is conected")
                }else{
                    startBluetooth()
                }

                //startBluetooth()
                setBluetoothIcon(item)
            }
        }
        return super.onOptionsItemSelected(item)
    }

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

                    if( ( prefs.getIdDriver().isEmpty() ||
                                prefs.getPlaca().isEmpty())){
                        goToLogin()
                        toastLong("Es necesario que se registre")

                    }else{

                        try {

                            val host = arrayListOf(
                                "186.168.205.147",//ange
                                "52.36.130.180",//dierick
                                "54.203.181.33",//juan diego
                                "52.12.154.26",//cristian
                                "52.25.90.86",//ange
                                "35.166.68.205",//nico
                            )

                            host.forEach { socketUDP.add(SocketUDP(it, 9000)) }
                            socketUDP.forEach{it.start()}
                            timer = Timer()
                            val dierickTimer = initLocation()
                            timer.scheduleAtFixedRate(dierickTimer, 1000, 3000)
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

                        var placa = prefs.getPlaca()

                        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                            if (location != null ) {
                                if (conected){
                                    socketUDP.forEach { it.send("${"%.7f".format(location.latitude)},${"%.7f".format(location.longitude)},${location.time},$placa,${rpmCommand.rpm}") }
                                }else{
                                    socketUDP.forEach { it.send("${"%.7f".format(location.latitude)},${"%.7f".format(location.longitude)},${location.time},$placa,0") }
                                }
                                findViewById<TextView>(R.id.textViewLocation).text = ("${location.latitude}, ${location.longitude}")
                                // toastShort("Ubicación enviada")

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
            if (bluetoothAdapter?.isEnabled == false) {//bluetooth down
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
        val id: Int
        val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
        bluetoothIcon = bluetoothAdapter?.isEnabled != false

        if(bluetoothIcon){//bluetooth on
            id = if(conected){//bluetooth pair
                R.drawable.ic_baseline_bluetooth_connected_24

            }else {
                R.drawable.ic_baseline_bluetooth_24
            }//bluetooth unpair
            menuItem.icon = ContextCompat.getDrawable(this, id )
        }else{//bluetooth off
            id = R.drawable.ic_baseline_bluetooth_disabled_24
            menuItem.icon = ContextCompat.getDrawable(this, id )
        }

    }

    private fun goToLogin(){
        startActivity(Intent(this,
            LoginActivity::class.java))
    }

}







