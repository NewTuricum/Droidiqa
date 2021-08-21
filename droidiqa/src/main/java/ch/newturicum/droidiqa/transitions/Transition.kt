package ch.newturicum.droidiqa.transitions

import ch.newturicum.droidiqa.network.request.TransactionData
import com.google.gson.Gson

/**
 * Created by dzorn on 08.06.21.
 */
open class Transition(private val name: String) {
    private val params = mutableListOf<TransitionParameter>()
    private val gson = Gson()

    fun addParameter(name: String, value: Any) {
        addParameter(TransitionParameter(vname = name, value = value))
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
        params.map { it.vname }.let { knownParams ->
            for (param in params) {
                if (!knownParams.contains(param.vname)) {
                    this.params.add(param)
                }
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