package ch.newturicum.droidiqa.dto.response

import ch.newturicum.droidiqa.database.dao.ZilTokenEntity

data class ZilSmartContractInit(
    val contractAddress: String,
    val name: String,
    val decimalPlaces: Int,
    val symbol: String,
    val supply: Long,
    var balances: Map<String, String>
)

internal fun ZilSmartContractInit.asTokenEntity(): ZilTokenEntity {
    return ZilTokenEntity(
        contractAddress = contractAddress,
        name = name,
        decimalPlaces = decimalPlaces,
        symbol = symbol,
        supply = supply,
        balances = balances
    )
}