package ch.newturicum.droidiqa.transitions

import ch.newturicum.droidiqa.network.request.TransactionData
import kotlinx.serialization.Serializable

@Serializable
open class Transition(
    val name: String,
    val params: MutableList<TransitionParameter> = mutableListOf()
) {
    fun addParameter(name: String, value: Any?) {
        addParameter(TransitionParameter.from(name, value))
    }

    fun addParameter(param: TransitionParameter) {
        if (!containsParameter(param.vname)) {
            params.add(param)
        }
    }

    fun getParameter(vname: String): TransitionParameter? {
        return params.find { it.vname == vname }
    }

    fun addParameters(params: Array<TransitionParameter>) {
        val knownParams = this.params.map { it.vname }
        for (param in params) {
            if (!knownParams.contains(param.vname)) {
                this.params.add(param)
            }
        }
    }

    private fun containsParameter(vname: String): Boolean {
        return params.map { it.vname }.contains(vname)
    }

    internal fun toTransactionData(senderAddress: String): TransactionData {
        return TransactionData(name, "0", senderAddress, senderAddress, params.toTypedArray())
    }
}