package com.kovalak.bakalarka

import com.kovalak.bakalarka.entities.Result
import com.kovalak.bakalarka.entities.ServerResult
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.security.KeyStore
import java.security.cert.Certificate
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocket
import javax.net.ssl.TrustManagerFactory
import kotlinx.serialization.json.Json
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

            val input = ObjectInputStream(socket.inputStream)

            val attestationChallenge = input.readObject() as ByteArray
            Timber.d("Received attestation challenge: ${attestationChallenge.toHexString()}")

            Timber.d("Sending certificate chain")
            val objectOutputStream = ObjectOutputStream(socket.outputStream)
            objectOutputStream.writeObject(startKeyAttestation(attestationChallenge))

            Timber.d("Getting response")
            val serverResultJson = input.readObject() as String
            val serverResult = Json.decodeFromString(ServerResult.serializer(), serverResultJson)

            if (serverResult.hasFailed) {
                Timber.e(serverResult.message)
                return Result.Failure(serverResult.message)
            }

            Timber.e("Key: ${serverResult.key?.toHexString()}")

            return Result.Success(serverResult.key ?: ByteArray(0))
        } catch (ex: Exception) {
            Timber.e(ex)
            return Result.Failure(ex.message ?: "Error")
        } finally {
            if (::socket.isInitialized) {
                socket.outputStream.close()
                socket.inputStream.close()
            }
        }
    }

    private fun createSSLContext(): SSLContext {
        val keyStore = KeyStore.getInstance(ANDROID_KEY_STORE)
        keyStore.load(null)
        keyStore.setCertificateEntry("cert", serverCertificate)

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