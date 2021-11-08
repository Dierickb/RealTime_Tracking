package com.example.gpslocationsms

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import com.example.gpslocationsms.UserLoginApplication.Companion.prefs
import java.util.*

class LoginActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        initUI()

        checkUserValues()

    }

    private fun checkUserValues(){
        val tvPlaca = findViewById<TextView>(R.id.tvPlaca)
        val tvName = findViewById<TextView>(R.id.tvName)
        val tvIdDriver = findViewById<TextView>(R.id.tvIdDriver)
        if (prefs.getUserName().isNotEmpty() && prefs.getIdDriver().isNotEmpty()
            && prefs.getPlaca().isNotEmpty()) {
            tvPlaca.text = ("Placa: " + prefs.getPlaca())
            tvIdDriver.text = ("ID: "+prefs.getIdDriver())
            tvName.text = ("Nombre: "+prefs.getUserName())
        }
    }

    private fun initUI(){
        val sendData = findViewById<Button>(R.id.send_data)
        sendData.setOnClickListener{
            if(accesToDetail()){
                goToMain()
            }
        }
    }

    private fun accesToDetail (): Boolean {
        val saved: Boolean

        val name = findViewById<EditText>(R.id.etName)
        val idDriver = findViewById<EditText>(R.id.etIdTaxi)
        val placaTaxi = findViewById<EditText>(R.id.etPlaca)

        saved = if (name.text.isNotEmpty() && idDriver.text.isNotEmpty() && placaTaxi.text.isNotEmpty()) {
            prefs.savePlaca(placaTaxi.text.toString().uppercase(Locale.getDefault()))
            prefs.saveIdDriver(idDriver.text.toString())
            prefs.saveDrivingName(name.text.toString().uppercase(Locale.getDefault()))
            toast("Los datos han sido guardados")
            true
        } else {
            toastLong("Datos vacios o invalidos")
            false
        }
        return saved
    }

    private fun goToMain(){
        startActivity(Intent(applicationContext,
            MainActivity::class.java))
    }


}


