package ch.newturicum.droidiqa.repository

import ch.newturicum.droidiqa.dto.ZilAccount
import ch.newturicum.droidiqa.dto.ZilToken
import ch.newturicum.droidiqa.dto.ZilTransaction
import ch.newturicum.droidiqa.dto.response.ZilSmartContractCode
import ch.newturicum.droidiqa.dto.response.ZilSmartContractInit

interface DroidiqaCallback {
    fun onError(message: String)
    fun onNetworkError()
}

interface CreateAccountCallback : DroidiqaCallback {
    fun onSuccess(account: ZilAccount)
}

interface AddTokenCallback : DroidiqaCallback {
    fun onSuccess(token: ZilToken)
}

interface TransitionCallback : DroidiqaCallback {
    fun onSuccess(transaction: ZilTransaction)
}

interface SmartContractInitCallback : DroidiqaCallback {
    fun onResult(result: ZilSmartContractInit?)
}

interface SmartContractCodeCallback : DroidiqaCallback {
    fun onResult(result: ZilSmartContractCode?)
}

interface MinimumGasPriceCallback : DroidiqaCallback {
    fun onResult(minGasPriceQa: Long, minGasPriceZil: Double)
}

interface RefreshCallback : DroidiqaCallback {
    fun onComplete(success: Boolean)
}