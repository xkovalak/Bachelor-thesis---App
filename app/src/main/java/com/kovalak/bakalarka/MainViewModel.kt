package com.kovalak.bakalarka

import android.app.Application
import android.content.SharedPreferences
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import androidx.security.crypto.MasterKeys
import com.hadilq.liveevent.LiveEvent
import java.security.KeyStore
import java.security.PrivateKey
import java.security.cert.CertificateFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val _result: LiveEvent<Result> = LiveEvent()
    val result: LiveData<Result>
        get() = _result

    private var masterKey: MasterKey

    init {
        val keyGenParameterSpec = MasterKeys.AES256_GCM_SPEC

        masterKey = MasterKey.Builder(getApplication(), MasterKey.DEFAULT_MASTER_KEY_ALIAS)
            .setKeyGenParameterSpec(keyGenParameterSpec)
            .build()
    }


    fun onConnect(ip: String) {
        _result.postValue(Result.Processing)

        val serverCertContent =
            getApplication<Application>().assets.open("server_cert.pem").readBytes()

        val certFactory = CertificateFactory.getInstance("X.509")
        val serverCert = certFactory.generateCertificate(serverCertContent.inputStream())

        viewModelScope.launch(Dispatchers.IO) {
            val res = HttpsClient(
                ip.takeIf { it.isNotBlank() } ?: "192.168.0.73",
                7879,
                "password".toCharArray(),
                serverCert,
            ).connect()

            _result.postValue(res)
        }
    }

    fun save3DESKey(receivedKey: ByteArray) {
        val keyStore = KeyStore.getInstance(ANDROID_KEY_STORE)
        keyStore.load(null)

        Timber.d("Is key entry: ${keyStore.isKeyEntry(KEY_ALIAS)}")

        val rsaKey = keyStore.getKey(KEY_ALIAS, null) as PrivateKey

        val decryptedKey = decrypt(rsaKey, "RSA/ECB/PKCS1Padding", receivedKey)
        Timber.d("Decrypted key: ${String(decryptedKey)}")

        val sharedPreferences = getEncryptedSharedPreferences()

        with(sharedPreferences.edit()) {
            putString(KEY_3DES, String(decryptedKey, Charsets.ISO_8859_1))
            apply()
        }
    }

    fun test() {
        val sharedPreferences = getEncryptedSharedPreferences()

        val key = sharedPreferences.getString(KEY_3DES, null)
        Timber.e("======: $key")
    }

    private fun getEncryptedSharedPreferences(): SharedPreferences =
        EncryptedSharedPreferences.create(
            getApplication(),
            SHARED_PREF_FILE,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )

    companion object {
        private const val KEY_3DES = "key_3des"
        private const val SHARED_PREF_FILE = "sharedPref"
    }
}