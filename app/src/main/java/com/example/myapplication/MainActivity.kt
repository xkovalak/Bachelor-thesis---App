package com.example.myapplication

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import java.io.File
import java.security.KeyFactory
import java.security.cert.CertificateFactory
import java.security.spec.PKCS8EncodedKeySpec
import java.util.Base64
import javax.crypto.SecretKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber

class MainActivity : AppCompatActivity() {

    private lateinit var ipEditText: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Timber.plant(Timber.DebugTree())

        setContentView(R.layout.activity_main)

        ipEditText = findViewById(R.id.ip)
        findViewById<Button>(R.id.connect_button).setOnClickListener { onConnect() }
    }

    private fun onConnect() {
//        if (ipEditText.text.isBlank()) {
//            return
//        }
        val serverCertContent = applicationContext.assets.open("server_cert.pem").readBytes()

        val certFactory = CertificateFactory.getInstance("X.509")
        val serverCert = certFactory.generateCertificate(serverCertContent.inputStream())

        CoroutineScope(Dispatchers.IO).launch {
            HttpsClient(
                ipEditText.text.toString().takeIf { it.isNotBlank() } ?: "192.168.0.73",
                7879,
                "password".toCharArray(),
                serverCert,
            ).connect()
        }
    }
}