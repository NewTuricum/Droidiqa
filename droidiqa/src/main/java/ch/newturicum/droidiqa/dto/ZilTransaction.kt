package ch.newturicum.droidiqa.dto

import com.google.gson.annotations.SerializedName
import java.io.Serializable

enum class ZilTransactionStatus {
    PENDING,
    FAILED,
    COMPLETED
}

enum class ZilTransactionType {
    ZIL_TRANSFER,
    ZRC2_TRANSFER,
    CALL_TRANSITION
}

data class ZilTransaction(
    @SerializedName("id")
    val hash: String,
    var status: ZilTransactionStatus,
    var timestamp: Long,
    var amount: Long = 0,
    var contract: String? = null,
    var receiver: String? = null
) : Serializable, Comparable<ZilTransaction> {
    override fun compareTo(other: ZilTransaction): Int {
        return hash.compareTo(other.hash)
    }

    override fun equals(other: Any?): Boolean {
        if (other is ZilTransaction) {
            return hash == other.hash
        }
        return false
    }
}

fun ZilTransaction.getType(): ZilTransactionType {
    return when {
        contract == null -> ZilTransactionType.ZIL_TRANSFER
        amount > 0 -> ZilTransactionType.ZRC2_TRANSFER
        else -> ZilTransactionType.CALL_TRANSITION
    }
}