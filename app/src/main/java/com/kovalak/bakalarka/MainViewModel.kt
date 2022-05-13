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
import com.kovalak.bakalarka.entities.Result
import java.security.KeyStore
import java.security.PrivateKey
import java.security.cert.CertificateFactory
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.SecretKeySpec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import timber.log.Timber


class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val _result: LiveEvent<Result> = LiveEvent()
    val result: LiveData<Result>
        get() = _result

    var serverJob: Job? = null

    fun onConnect(ip: String) {
        _result.postValue(Result.Processing)

        val serverCertContent =
            getApplication<Application>().assets.open("server_cert.pem").readBytes()

        val certFactory = CertificateFactory.getInstance("X.509")
        val serverCert = certFactory.generateCertificate(serverCertContent.inputStream())

        serverJob = viewModelScope.launch(Dispatchers.IO) {
            val res = HttpsClient(
                ip.takeIf { it.isNotBlank() } ?: "192.168.0.73",
                7879,
                "password".toCharArray(),
                serverCert,
            ).connect()

            if (this.isActive) {
                _result.postValue(res)
            } else {
                Timber.d("Job was cancelled, ignoring result")
            }
        }
    }

    fun save3DESKey(receivedKey: ByteArray) {
        Timber.d("Saving key...")
        val sharedPreferences = getEncryptedSharedPreferences()

        with(sharedPreferences.edit()) {
            putString(KEY_3DES, String(receivedKey, Charsets.ISO_8859_1))
            apply()
        }
    }

    fun get3DESKey(): SecretKey? {
        val keyStore = KeyStore.getInstance(ANDROID_KEY_STORE).also {
            it.load(null)
        }

        val rsaKey = keyStore.getKey(KEY_ALIAS, null) as PrivateKey
        val sharedPreferences = getEncryptedSharedPreferences()

        val encryptedKey = sharedPreferences.getString(KEY_3DES, null) ?: return null

        val decryptedKey =
            decrypt(rsaKey, "RSA/ECB/PKCS1Padding", encryptedKey.toByteArray(Charsets.ISO_8859_1))
        Timber.d("Decrypted key: ${decryptedKey.toHexString()}")

        val keySpec =
            SecretKeySpec(decryptedKey, "DESede")
        val tripleDesKey = SecretKeyFactory.getInstance("DESede").generateSecret(keySpec)

        return tripleDesKey
    }

    private fun getEncryptedSharedPreferences(): SharedPreferences {
        val keyGenParameterSpec = MasterKeys.AES256_GCM_SPEC

        val masterKey = MasterKey.Builder(getApplication(), MasterKey.DEFAULT_MASTER_KEY_ALIAS)
            .setKeyGenParameterSpec(keyGenParameterSpec)
            .build()

        return EncryptedSharedPreferences.create(
            getApplication(),
            SHARED_PREF_FILE,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun onCancel() {
        serverJob?.cancelChildren()
        serverJob?.cancel()
        Timber.d("Canceling...")
        _result.postValue(Result.Failure("Canceled by user"))
    }

    companion object {
        private const val KEY_3DES = "key_3des"
        private const val SHARED_PREF_FILE = "sharedPref"
    }
}