package ch.newturicum.droidiqa.network.request

import ch.newturicum.droidiqa.network.ZILLIQA

internal data class RequestData(
    val id: String = ZILLIQA.API_ID,
    val jsonrpc: String = ZILLIQA.JSON_RPC,
    val method: String,
    var params: Array<String> = arrayOf("")
)

