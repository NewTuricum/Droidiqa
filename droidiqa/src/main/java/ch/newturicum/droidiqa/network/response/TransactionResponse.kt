package ch.newturicum.droidiqa.network.response

import ch.newturicum.droidiqa.dto.ZilTransaction
import ch.newturicum.droidiqa.dto.ZilTransactionStatus
import ch.newturicum.droidiqa.network.request.SingleTransaction
import ch.newturicum.droidiqa.transitions.Transition
import ch.newturicum.droidiqa.transitions.ZRC2TokenTransfer
import ch.newturicum.droidiqa.util.DroidiqaUtils

internal data class TransactionRequestResult(
    val Info: String,
    val TranID: String
)

internal class TransactionResponse : BaseDto<TransactionRequestResult>()

internal fun TransactionResponse.toZilTransaction(transaction: SingleTransaction): ZilTransaction? {
    result?.let {
        return ZilTransaction(
            it.TranID,
            ZilTransactionStatus.PENDING,
            System.currentTimeMillis(),
            transaction.amount.toLong(),
            null,
            DroidiqaUtils.toBech32Address(transaction.toAddr)
        )
    }
    return null
}

internal fun TransactionResponse.toZilTransaction(
    contract: String,
    transition: Transition
): ZilTransaction? {
    result?.let {
        var amount = 0L
        var receiver: String? = null
        if (transition is ZRC2TokenTransfer) {
            amount = transition.getAmount()
            receiver = DroidiqaUtils.toBech32Address(transition.getReceiverAddress())
        }

        return ZilTransaction(
            it.TranID,
            ZilTransactionStatus.PENDING,
            System.currentTimeMillis(),
            amount,
            DroidiqaUtils.toBech32Address(contract),
            receiver
        )
    }
    return null
}