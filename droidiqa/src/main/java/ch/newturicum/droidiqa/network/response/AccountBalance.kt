package ch.newturicum.droidiqa.network.response

import ch.newturicum.droidiqa.util.DroidiqaUtils

internal data class AccountBalanceResult(
    val balance: String,
    val nonce: Int
)

internal class AccountBalance : BaseDto<AccountBalanceResult>()

internal fun AccountBalance.doubleBalance(): Double {
    result?.apply {
        return DroidiqaUtils.qaToZil(balance.toLong())
    }
    return 0.0
}