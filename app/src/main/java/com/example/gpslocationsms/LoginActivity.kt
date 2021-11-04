package com.example.gpslocationsms

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import com.example.gpslocationsms.UserLoginApplication.Companion.prefs

class LoginActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        initUI()

        checkUserValues()

    }

    fun checkUserValues(){
        if( (prefs.getUser().isEmpty() || prefs.getIdDriver()==0 ||
                    prefs.getPlaca().isEmpty()) ){
            val tvPlaca = findViewById<TextView>(R.id.tvPlaca)
            val tvName = findViewById<TextView>(R.id.tvName)
            val tvIdDriver = findViewById<TextView>(R.id.tvIdDriver)

            tvPlaca.text = prefs.getPlaca()
            tvName.text = prefs.getUser()
            tvIdDriver.text = prefs.getIdDriver().toString()
        }
    }

    fun initUI(){
        val sendData = findViewById<Button>(R.id.send_data)
        sendData.setOnClickListener{
            if(accesToDetail()){
                goToMain()
            }
            val name = findViewById<EditText>(R.id.etName)
            prefs.saveUser(name.text.toString())
            toastLong(name.text.toString())
        }
    }

    fun accesToDetail (): Boolean {
        var saved: Boolean

        val name = findViewById<EditText>(R.id.etName)
        val idDriver = findViewById<EditText>(R.id.etIdTaxi)
        val placaTaxi = findViewById<EditText>(R.id.etPlaca)

        if( (name.text.isEmpty() || idDriver.text.isEmpty() ||
                    placaTaxi.text.isEmpty())){
            toastLong("Datos vacios o invalidos")
            saved = false
        }else{
            prefs.saveUser(name.text.toString())
            prefs.savePlaca(placaTaxi.text.toString())
            prefs.saveIdDriver(placaTaxi.inputType)
            toast("Los datos han sido guardados")
            saved = true
        }
        return saved
    }

    fun goToMain(){
        startActivity(Intent(applicationContext,
            MainActivity::class.java))
    }


}

