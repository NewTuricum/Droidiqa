package ch.newturicum.droidiqa.dto

import ch.newturicum.droidiqa.util.fromBech32Address
import ch.newturicum.droidiqa.util.hexPrefixed
import java.io.Serializable
import java.util.*

data class ZilToken(
    val name: String,
    val address: String,
    val symbol: String,
    val decimalPlaces: Int,
    val supply: Long,
    val balances: Map<String, String>
) : Serializable {
    fun getBalanceForAccount(account: ZilAccount): Double =
        getBalanceForAccount(account.address)

    fun getBalanceForAccount(accountAddress: String): Double {
        balances[accountAddress.fromBech32Address().lowercase(Locale.ENGLISH).hexPrefixed()]?.let {
            if (it.isNotEmpty()) {
                return it.toDouble()
            }
        }
        return 0.0
    }
}