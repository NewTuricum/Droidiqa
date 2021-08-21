package ch.newturicum.droidiqa.util

import android.security.keystore.KeyProperties
import ch.newturicum.droidiqa.Constants
import com.firestack.laksaj.account.Account
import com.firestack.laksaj.crypto.KeyTools
import com.firestack.laksaj.utils.Validation
import java.math.BigInteger
import java.security.KeyPairGenerator
import java.security.spec.ECGenParameterSpec
import java.util.regex.Pattern

class DroidiqaUtils {
    companion object {

        enum class Units(val multiplier: Long) {
            ZIL(1000000000000),
            LI(1000000),
            QA(1)
        }

        fun looksLikeBech32Address(string: String?): Boolean {
            if (string.isNullOrEmpty()) {
                return false
            }
            return string.startsWith(Constants.ZIL_HRP) && string.length == Constants.ZIL_ADDRESS_LEN
        }

        fun isValidAddress(address: String?): Boolean {
            address?.let {
                return Validation.isAddress(address).or(isBech32(address))
            }
            return false
        }

        fun isBech32(str: String?): Boolean {
            val pattern = Pattern.compile("^zil1[qpzry9x8gf2tvdw0s3jn54khce6mua7l]{38}")
            val matcher = pattern.matcher(str)
            return matcher.matches()
        }

        fun isPrivateKey(privateKey: String?): Boolean {
            return Validation.isByteString(privateKey, 64)
        }

        fun isValidChecksumAddress(address: String): Boolean {
            val truncatedAddress = address.replace("0x", "")
            return Validation.isAddress(truncatedAddress) && Account.toCheckSumAddress(
                truncatedAddress
            ) == address
        }

        fun isValidBech32Address(value: String?): Boolean =
            !value.isNullOrEmpty() && Validation.isBech32(value)


        fun bech32AddressFromPublicKey(publicKey: String): String? {
            return toBech32Address(KeyTools.getAddressFromPublicKey(publicKey))
        }

        fun toBech32Address(address: String?): String? {
            if (isValidBech32Address(address)) {
                return address
            }
            address?.let {
                return if (it.startsWith("0x"))
                    com.firestack.laksaj.utils.Bech32.toBech32Address(it)
                else
                    com.firestack.laksaj.utils.Bech32.toBech32Address("0x$it")
            }
            return null
        }

        fun fromBech32Address(address: String?): String? {
            return if (isValidBech32Address(address)) {
                "0x" + com.firestack.laksaj.utils.Bech32.fromBech32Address(address)
            } else null
        }

        fun zilToQa(zil: Double): Long {
            return (Units.ZIL.multiplier * zil).toLong()
        }

        fun qaToZil(qa: Long): Double {
            return qa / Units.ZIL.multiplier.toDouble()
        }

        internal fun generatePrivateKey(): String {
            val kpg = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_EC)
            kpg.initialize(ECGenParameterSpec("secp256r1"))
            val kp = kpg.generateKeyPair()
            val bi = BigInteger(kp.private.encoded).toString(16)
            return bi.substring(bi.length - 64)
        }
    }
}


