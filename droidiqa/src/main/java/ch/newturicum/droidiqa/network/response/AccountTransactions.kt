package ch.newturicum.droidiqa.network.response

import ch.newturicum.droidiqa.dto.ZilTransaction
import ch.newturicum.droidiqa.dto.ZilTransactionStatus

internal data class TransactionReceipt(
    val cumulative_gas: String,
    val epoch_num: String,
    val success: Boolean
)

internal data class GetTransactionResponseData(
    val ID: String,
    val amount: String,
    val gasLimit: String,
    val gasPrice: String,
    val nonce: String,
    val receipt: TransactionReceipt?
)


internal class GetTransactionResponse : BaseDto<GetTransactionResponseData>() {
    lateinit var senderPubKey: String
    lateinit var signature: String
    lateinit var toAddr: String
    lateinit var version: String
}

internal fun GetTransactionResponse.updateZilTransaction(transaction: ZilTransaction) {
    if (isError()) {
        return
    }
    getStatus()?.let {
        transaction.status = it
    }
}

internal fun GetTransactionResponse.getStatus(): ZilTransactionStatus? {
    result?.let {
        return when {
            it.receipt == null -> ZilTransactionStatus.PENDING
            it.receipt.success -> ZilTransactionStatus.COMPLETED
            else -> ZilTransactionStatus.FAILED
        }
    }
    return null
}