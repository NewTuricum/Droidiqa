package ch.newturicum.droidiqa.network.request

import ch.newturicum.droidiqa.Constants.ZIL_CHAINID_MAIN
import ch.newturicum.droidiqa.Constants.ZIL_MESSAGE_VERSION
import ch.newturicum.droidiqa.network.ZILLIQA
import ch.newturicum.droidiqa.network.response.TransactionResponse
import ch.newturicum.droidiqa.transitions.TransitionParameter
import ch.newturicum.droidiqa.util.extensions.pack
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.json.JSONObject

@Serializable
internal data class SingleTransaction(
    val version: Int = ZIL_CHAINID_MAIN.pack(ZIL_MESSAGE_VERSION),
    val nonce: Int,
    var toAddr: String,
    var amount: String,
    var pubKey: String,
    var gasPrice: String,
    var gasLimit: String,
    var code: String? = null,
    var data: String? = null,
    var signature: String,
    var priority: Boolean? = null
)

@Serializable
internal data class TransactionData(
    val _tag: String? = null,
    val _amount: String? = null,
    val _sender: String? = null,
    val _origin: String? = null,
    val params: Array<TransitionParameter>
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as TransactionData

        if (_tag != other._tag) return false
        if (_amount != other._amount) return false
        if (_sender != other._sender) return false
        if (_origin != other._origin) return false
        if (!params.contentEquals(other.params)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = _tag?.hashCode() ?: 0
        result = 31 * result + (_amount?.hashCode() ?: 0)
        result = 31 * result + (_sender?.hashCode() ?: 0)
        result = 31 * result + (_origin?.hashCode() ?: 0)
        result = 31 * result + params.contentHashCode()
        return result
    }
}

@Serializable
internal data class TransactionRequestData(
    val id: String = ZILLIQA.API_ID,
    val jsonrpc: String = ZILLIQA.JSON_RPC,
    val method: String,
    var params: Array<SingleTransaction>
)

internal class TransactionRequest(
    apiRoot: String,
    transaction: SingleTransaction,
    responseListener: Response.Listener<TransactionResponse>,
    errorListener: Response.ErrorListener
) : JsonObjectRequest(
    Method.POST,
    apiRoot,
    JSONObject(
        Json.encodeToString(
            TransactionRequestData(
                method = ZILLIQA.METHOD.CREATE_TRANSACTION,
                params = arrayOf(transaction)
            )
        )
    ),
    Response.Listener { response ->
        responseListener.onResponse(
            Json.decodeFromString<TransactionResponse>(response.toString())
        )
    },
    Response.ErrorListener { error ->
        errorListener.onErrorResponse(error)
    }
)