package com.kovalak.bakalarka

import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.security.KeyStore
import java.security.cert.Certificate
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocket
import javax.net.ssl.TrustManagerFactory
import timber.log.Timber

class HttpsClient(
    private val ip: String,
    private val port: Int,
    private val password: CharArray,
    private val serverCertificate: Certificate,
) {

    private lateinit var socket: SSLSocket

    fun connect(): Result {
        try {
            val sslContext = createSSLContext()
            Timber.d("SSLContext created")

            val socketFactory = sslContext.socketFactory
            Timber.d("Connecting to $ip:$port")
            socket = socketFactory.createSocket(ip, port) as SSLSocket
            Timber.d("Socket created")
            Timber.d("Supported CipherSuites: ${socket.supportedCipherSuites}")
            Timber.d("Enabled CipherSuites: ${socket.enabledCipherSuites}")

            Timber.d("Start handshake")
            socket.startHandshake()

            Timber.d("Local certificates: ${socket.session.localCertificates}")
            Timber.d("Peer certificates: ${socket.session.peerCertificates}")

            val output = ObjectOutputStream(socket.outputStream)
            Timber.d("Sending certificate chain")
            output.writeObject(startKeyAttestation())

            Timber.d("Receiving key")
            val input = ObjectInputStream(socket.inputStream)
            val receivedKey = input.readObject() as ByteArray

            Timber.e("Key: ${String(receivedKey)}")

            return Result.Success(receivedKey)
        } catch (ex: Exception) {
            Timber.e(ex)
            return Result.Failure(ex.message ?: "Error")
        }
    }

    private fun createSSLContext(): SSLContext {
        val keyStore = KeyStore.getInstance(ANDROID_KEY_STORE)
        keyStore.load(null)
        Timber.d("KeyStore size: ${keyStore.size()}")
        keyStore.setCertificateEntry("cert", serverCertificate)
        Timber.d("KeyStore size: ${keyStore.size()}")

        val keyManager = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm())
        keyManager.init(keyStore, password)

        val trustManagerFactory =
            TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
        trustManagerFactory.init(keyStore)

        val context = SSLContext.getInstance("TLSv1.2")
        context.init(keyManager.keyManagers, trustManagerFactory.trustManagers, null)

        return context
    }
}