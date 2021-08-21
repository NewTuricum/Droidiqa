package ch.newturicum.droidiqa.network.response

import ch.newturicum.droidiqa.util.fromBech32Address
import ch.newturicum.droidiqa.util.hexPrefixed
import java.util.*

internal data class SmartContractStateResult(
    val _balance: String,
    val admin: String,
    val allowances: Map<String, Map<String, String>>?,
    val balances: Map<String, String>?,
    val implementation: String,
    val total_supply: String
)

internal class SmartContractState : BaseDto<SmartContractStateResult>()

internal fun SmartContractState.getTotalSupply(): Int = result?.total_supply?.toInt() ?: 0

internal fun SmartContractState.getBalance(accountAddress: String): Float {
    val bytAddress = accountAddress.fromBech32Address().hexPrefixed().lowercase(Locale.ENGLISH)
    result?.balances?.get(bytAddress)?.let {
        return it.toFloat()
    }
    return 0f
}

internal fun SmartContractState.getDoubleBalance(accountAddress: String): Double {
    val bytAddress = accountAddress.fromBech32Address().hexPrefixed().lowercase(Locale.ENGLISH)
    result?.balances?.get(bytAddress)?.let {
        return it.toDouble()
    }
    return 0.0
}