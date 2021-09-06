package com.example.gpslocationsms


import android.Manifest
import android.annotation.SuppressLint
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.telephony.SmsManager
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices

class MainActivity : AppCompatActivity() {

    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient

    private val coarseLocationPermission = PermissionLocationRequester(
        this,
        Manifest.permission.ACCESS_FINE_LOCATION,
        onRationale = {toastLong("Para obtener la ubicaíon es necesario el permiso de Location")},
        onDenied = {openAppSettings()
            toastLong("Dirijase a permisos, en location y active la localización para obtener su ubicación")}
    )

    private val coarseSendMessagePermission = PermissionSendMessageRequester(
        this,
        Manifest.permission.SEND_SMS,
        onRationale = {toastLong("Es necesario el permiso de SMS para enviar la ubicación")},
        onDenied = {openAppSettings()
            toastLong("Dirijase a permisos, en SMS y seleccione activar para enviar su ubicación por SMS")}
    )

    @SuppressLint("MissingPermission")
    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val phoneNumber = findViewById<EditText>(R.id.editTextPhone)
        val getLocation = findViewById<Button>(R.id.buttonObtainLocation)
        val sendLocation = findViewById<Button>(R.id.buttonSendLocation)
        val textViewLocation = findViewById<TextView>(R.id.textViewLocation)

        getLocation.setOnClickListener{
            coarseLocationPermission.runWithPermission{
                fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
                val task = fusedLocationProviderClient.lastLocation
                task.addOnSuccessListener {
                    if(it != null){
                        val latitude  = it.latitude
                        val longitude = it.longitude
                        textViewLocation.text = ("$latitude, $longitude")
                    }
                }
            }
        }

        sendLocation.setOnClickListener{
            coarseSendMessagePermission.runWithPermission{

                SmsManager.getDefault().sendTextMessage(
                    "${phoneNumber.text}", null,
                    "${textViewLocation.text}",  null, null)
                toastLong("Message sended at: ${phoneNumber.text}")
            }
        }
    }
}

class PermissionLocationRequester(
    activity: ComponentActivity,
    private val permission: String,
    private val onRationale: () -> Unit = {},
    private val onDenied: () -> Unit = {}
) {
    private var onGranted: () -> Unit = {}

    private val permissionLauncher = activity.registerForActivityResult(RequestPermission()){ isGranted ->
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            when{
                isGranted -> onGranted()
                activity.shouldShowRequestPermissionRationale(permission) ->
                    onRationale()
                else -> onDenied()
            }
        }
    }
    fun runWithPermission(body:() -> Unit){
        onGranted = body
        permissionLauncher.launch(permission)
    }
}

class PermissionSendMessageRequester(
    activity: ComponentActivity,
    private val permission: String,
    private val onRationale: () -> Unit = {},
    private val onDenied: () -> Unit = {}
) {
    private var onGranted: () -> Unit = {}
    private val permissionLauncher = activity.registerForActivityResult(RequestPermission()){ isGranted ->
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            when{
                isGranted -> onGranted()
                activity.shouldShowRequestPermissionRationale(permission) ->
                    onRationale()
                else -> onDenied()
            }
        }
    }
    fun runWithPermission(body:() -> Unit){
        onGranted = body
        permissionLauncher.launch(permission)
    }
}




