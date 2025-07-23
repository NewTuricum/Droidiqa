package ch.newturicum.droidiqa.network.request

import ch.newturicum.droidiqa.network.ZILLIQA
import kotlinx.serialization.Serializable

@Serializable
internal data class RequestData(
    val id: String = ZILLIQA.API_ID,
    val jsonrpc: String = ZILLIQA.JSON_RPC,
    val method: String,
    var params: Array<String> = arrayOf("")
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as RequestData

        if (id != other.id) return false
        if (jsonrpc != other.jsonrpc) return false
        if (method != other.method) return false
        if (!params.contentEquals(other.params)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + jsonrpc.hashCode()
        result = 31 * result + method.hashCode()
        result = 31 * result + params.contentHashCode()
        return result
    }
}

