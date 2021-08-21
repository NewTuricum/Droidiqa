package ch.newturicum.droidiqa.database.dao

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import ch.newturicum.droidiqa.dto.ZilAccount
import ch.newturicum.droidiqa.dto.ZilTransaction
import ch.newturicum.droidiqa.security.KeyEncoder
import ch.newturicum.droidiqa.util.DroidiqaUtils
import com.firestack.laksaj.crypto.Schnorr
import com.firestack.laksaj.transaction.Transaction
import com.firestack.laksaj.utils.Bech32
import org.web3j.crypto.ECKeyPair
import java.io.IOException
import java.math.BigInteger
import java.security.NoSuchAlgorithmException
import java.util.*

@Entity
internal data class ZilAccountEntity(
    @PrimaryKey val encryptedPrivateKey: String,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "address") val address: String,
    @ColumnInfo(name = "publicKey") val publicKey: String,
    @ColumnInfo(name = "nonce") var nonce: Int,
    @ColumnInfo(name = "zilBalance") var zilBalance: Double,
    @ColumnInfo(name = "transactions") var transactions: List<ZilTransaction>
) : Comparable<ZilAccountEntity> {
    override fun compareTo(other: ZilAccountEntity): Int = address.compareTo(other.address)
    override fun equals(other: Any?): Boolean {
        return when (other) {
            is ZilAccountEntity -> compareTo(other) == 0
            is ZilAccount -> address == other.address
            else -> super.equals(other)
        }
    }

    fun toZilAccount(): ZilAccount {
        return ZilAccount(
            name, address, zilBalance
        )
    }

    fun keyPair(keyEncoder: KeyEncoder): ECKeyPair? {
        keyEncoder.decode(encryptedPrivateKey)?.let { privateKey ->
            return ECKeyPair(BigInteger(privateKey, 16), BigInteger(publicKey, 16))
        }
        return null
    }

    @Throws(Exception::class)
    fun sign(transaction: Transaction, keyEncoder: KeyEncoder): Transaction? {
        if (transaction.toAddr.startsWith("0x") || transaction.toAddr.startsWith("0X")) {
            transaction.toAddr = transaction.toAddr.substring(2)
        }
        if (!DroidiqaUtils.isBech32(transaction.toAddr) && !DroidiqaUtils.isValidChecksumAddress("0x" + transaction.toAddr)) {
            throw Exception("not checksum address or bech32")
        }
        if (DroidiqaUtils.isBech32(transaction.toAddr)) {
            transaction.toAddr = Bech32.fromBech32Address(transaction.toAddr)
        }
        if (DroidiqaUtils.isValidChecksumAddress("0x" + transaction.toAddr)) {
            transaction.toAddr = "0x" + transaction.toAddr
        }
        val txParams = transaction.toTransactionParam()
        if (Objects.nonNull(txParams) && txParams.senderPubKey.isNotEmpty()) {
            return signWith(transaction, this, keyEncoder)
        }
        return signWith(transaction, this, keyEncoder)
    }

    @Throws(IOException::class, NoSuchAlgorithmException::class)
    fun signWith(
        tx: Transaction,
        signer: ZilAccountEntity?,
        keyEncoder: KeyEncoder
    ): Transaction? {
        require(!Objects.isNull(signer)) { "account must not be null" }
        tx.nonce = (++signer!!.nonce).toString()
        tx.senderPubKey = signer.publicKey
        val message = tx.bytes()

        val signature = Schnorr.sign(signer.keyPair(keyEncoder), message)
        tx.signature = signature.toString().lowercase(Locale.ENGLISH)
        return tx
    }

    fun addTransaction(transaction: ZilTransaction) {
        if (transactions.contains(transaction)) {
            updateTransaction(transaction)
        } else {
            mutableListOf<ZilTransaction>().apply {
                add(transaction)
                addAll(transactions)
                transactions = this.toList()
            }
        }
    }

    private fun updateTransaction(transaction: ZilTransaction) {
        transactions.find { it.hash == transaction.hash }?.let {
            it.status = transaction.status
        }
    }
}

internal fun ZilAccountEntity.newName(name: String?): ZilAccountEntity {
    val newName = name ?: this.name
    return ZilAccountEntity(
        name = newName,
        address = address,
        publicKey = publicKey,
        encryptedPrivateKey = encryptedPrivateKey,
        zilBalance = zilBalance,
        nonce = nonce,
        transactions = transactions
    )
}