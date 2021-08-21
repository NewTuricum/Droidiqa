package ch.newturicum.droidiqa.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Default symmetric key encoder implementation.
 */
class KeyEncoderImpl : KeyEncoder {

    companion object {
        const val KEYSTORE = "AndroidKeyStore"
        const val IV_LEN = 12
        const val SECRET_KEY_ALIAS = "zilPrivKeySecKey2"
        const val CIPHER_TRANSFORM = "AES/GCM/NoPadding"
    }

    private val keyStore = KeyStore.getInstance(KEYSTORE)

    init {
        keyStore.load(null)
    }

    private fun getSecretKey(): SecretKey? {
        try {
            keyStore.getEntry(SECRET_KEY_ALIAS, null)?.let {
                if (it is KeyStore.SecretKeyEntry) {
                    return it.secretKey
                }
            }
            return initSecretKey()
        } catch (e: Exception) {
            // No key found - create new one!
            return initSecretKey()
        }
    }

    private fun initSecretKey(): SecretKey {
        val keySpecs = KeyGenParameterSpec.Builder(
            SECRET_KEY_ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        ).setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build()
        val kg = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE)

        kg.init(keySpecs)
        return kg.generateKey()
    }

    private fun encryptData(data: ByteArray): ByteArray? {
        getSecretKey()?.let { secretKey ->
            val cipher = Cipher.getInstance(CIPHER_TRANSFORM)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)
            cipher.doFinal(data)?.let {
                return it.plus(cipher.iv)
            }
            return null
        }
        return null
    }

    private fun decryptData(data: ByteArray): ByteArray? {
        getSecretKey()?.let { secretKey ->
            val input = data.copyOfRange(0, data.size - IV_LEN)
            val iv = data.copyOfRange(data.size - IV_LEN, data.lastIndex + 1)
            val cipher = Cipher.getInstance(CIPHER_TRANSFORM)
            val gcmParamSpec = GCMParameterSpec(128, iv)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmParamSpec)
            return cipher.doFinal(input)
        }
        return null
    }

    override fun encode(value: String): String? {
        encryptData(value.encodeToByteArray())?.let {
            return Base64.encodeToString(it, Base64.DEFAULT)
        }
        return null
    }

    override fun decode(value: String): String? {
        decryptData(Base64.decode(value, Base64.DEFAULT))?.let {
            return String(it)
        }
        return null
    }
}