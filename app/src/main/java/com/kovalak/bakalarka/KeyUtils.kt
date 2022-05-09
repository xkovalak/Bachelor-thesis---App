package com.kovalak.bakalarka

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.math.BigInteger
import java.security.Key
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.cert.X509Certificate
import java.util.Random
import javax.crypto.Cipher
import timber.log.Timber

fun startKeyAttestation(): Array<X509Certificate> {
    generateKeyToAttest(KEY_ALIAS, KeyProperties.KEY_ALGORITHM_RSA)

    val keyStore = KeyStore.getInstance(ANDROID_KEY_STORE).also {
        it.load(null)
    }

    val certificateChain = keyStore.getCertificateChain(KEY_ALIAS).map { it as X509Certificate }

    return certificateChain.toTypedArray()
}

private fun generateKeyToAttest(alias: String, algorithm: String) {
    val purpose =
        KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY or KeyProperties.PURPOSE_DECRYPT
    val keyGenParameterSpec = KeyGenParameterSpec.Builder(alias, purpose)
        .setKeySize(1024)
        .setAttestationChallenge(BigInteger(128, Random()).toByteArray())
        .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_RSA_PKCS1)
        .build()

    KeyPairGenerator.getInstance(algorithm, ANDROID_KEY_STORE).apply {
        initialize(keyGenParameterSpec)
        generateKeyPair()
    }
}

fun decrypt(key: Key, algorithm: String, encryptedMessage: ByteArray): ByteArray {
    val cipher = Cipher.getInstance(algorithm)
    cipher.init(Cipher.DECRYPT_MODE, key)
    val decryptedMessage = cipher.doFinal(encryptedMessage)
    Timber.d("Decrypted message: ${String(decryptedMessage)}")

    return decryptedMessage
}

fun encrypt(key: Key, algorithm: String, message: ByteArray): ByteArray {
    val cipher = Cipher.getInstance(algorithm)
    cipher.init(Cipher.ENCRYPT_MODE, key)
    val encryptedMessage = cipher.doFinal(message)
    Timber.d("Encrypted message: ${String(encryptedMessage)}")

    return encryptedMessage
}

const val ANDROID_KEY_STORE = "AndroidKeyStore"
const val KEY_ALIAS = "key_to_attest"