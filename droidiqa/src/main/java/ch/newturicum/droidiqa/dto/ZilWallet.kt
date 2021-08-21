package ch.newturicum.droidiqa.dto

import ch.newturicum.droidiqa.network.ZilNetwork
import ch.newturicum.droidiqa.util.DroidiqaUtils
import java.io.Serializable

data class ZilWallet(
    val network: ZilNetwork,
    val activeAccount: ZilAccount?,
    val accounts: List<ZilAccount>,
    val tokens: List<ZilToken>
) : Serializable {
    fun getZilBalance(): Double = DroidiqaUtils.qaToZil((activeAccount?.zilBalance ?: 0.0).toLong())
}