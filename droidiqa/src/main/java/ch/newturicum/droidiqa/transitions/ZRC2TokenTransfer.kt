package ch.newturicum.droidiqa.transitions

/**
 * Created by dzorn on 08.06.21.
 */

/**
 * Standardized ZRC2-token transfer.
 * @param receiverAddress The address the tokens are supposed to be sent to.
 * @param amount The amount of tokens to send.
 */
class ZRC2TokenTransfer(receiverAddress: String, amount: Long) :
    Transition("Transfer") {

    init {
        addParameter("amount", amount)
        addParameter("to", receiverAddress)
    }

    fun getAmount(): Long {
        return when (val value = getParameter("amount")!!.value) {
            is String -> value.toLong()
            is Long -> value
            else -> 0L
        }
    }

    fun getReceiverAddress(): String = getParameter("to")!!.value as String
}