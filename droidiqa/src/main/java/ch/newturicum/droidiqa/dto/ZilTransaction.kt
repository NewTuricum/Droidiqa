package ch.newturicum.droidiqa.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class ZilTransactionStatus {
    PENDING,
    FAILED,
    COMPLETED
}

@Serializable
enum class ZilTransactionType {
    ZIL_TRANSFER,
    ZRC2_TRANSFER,
    CALL_TRANSITION
}

@Serializable
data class ZilTransaction(
    @SerialName("id")
    val hash: String,
    var status: ZilTransactionStatus,
    var timestamp: Long,
    var amount: Long = 0,
    var contract: String? = null,
    var receiver: String? = null
) : Comparable<ZilTransaction> {
    override fun compareTo(other: ZilTransaction): Int {
        return hash.compareTo(other.hash)
    }

    override fun equals(other: Any?): Boolean {
        if (other is ZilTransaction) {
            return hash == other.hash
        }
        return false
    }

    override fun hashCode(): Int {
        return hash.hashCode()
    }
}

fun ZilTransaction.getType(): ZilTransactionType {
    return when {
        contract == null -> ZilTransactionType.ZIL_TRANSFER
        amount > 0 -> ZilTransactionType.ZRC2_TRANSFER
        else -> ZilTransactionType.CALL_TRANSITION
    }
}