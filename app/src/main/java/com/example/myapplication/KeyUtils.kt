package com.example.myapplication

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.math.BigInteger
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.cert.X509Certificate
import java.util.Random
import timber.log.Timber

fun startKeyAttestation(): Array<X509Certificate> {
    generateKey(KEY_ALIAS, KeyProperties.KEY_ALGORITHM_AES)

    val keyStore = KeyStore.getInstance(ANDROID_KEY_STORE).also {
        it.load(null, null)
    }
    val certificateChain = keyStore.getCertificateChain(KEY_ALIAS).map { it as X509Certificate }
    certificateChain.forEach {
        Timber.d("Attestation", Base64.encodeToString(it.encoded, Base64.NO_WRAP))
    }

    return certificateChain.toTypedArray()
}

private fun generateKey(alias: String, algorithm: String) {
    val purpose = KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY
    val keyGenParameterSpec = KeyGenParameterSpec.Builder(alias, purpose)
        .setKeySize(256)
        .setAttestationChallenge(BigInteger(128, Random()).toByteArray())
        .build()

    KeyPairGenerator.getInstance(algorithm, ANDROID_KEY_STORE).apply {
        initialize(keyGenParameterSpec)
        genKeyPair()
    }
}

private const val ANDROID_KEY_STORE = "AndroidKeyStore"
private const val KEY_ALIAS = "key_to_attest"