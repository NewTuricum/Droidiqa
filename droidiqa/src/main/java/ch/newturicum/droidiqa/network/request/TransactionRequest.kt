package ch.newturicum.droidiqa.network.request

import ch.newturicum.droidiqa.Constants.ZIL_CHAINID_MAIN
import ch.newturicum.droidiqa.Constants.ZIL_MESSAGE_VERSION
import ch.newturicum.droidiqa.network.ZILLIQA
import ch.newturicum.droidiqa.network.response.TransactionResponse
import ch.newturicum.droidiqa.transitions.TransitionParameter
import ch.newturicum.droidiqa.util.extensions.pack
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.google.gson.Gson
import org.json.JSONObject

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

internal data class TransactionData(
    val _tag: String?,
    val _amount: String?,
    val _sender: String?,
    val _origin: String?,
    val params: Array<TransitionParameter>
)

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
        Gson().toJson(
            TransactionRequestData(
                method = ZILLIQA.METHOD.CREATE_TRANSACTION,
                params = arrayOf(transaction)
            )
        )
    ),
    Response.Listener { response ->
        responseListener.onResponse(
            Gson().fromJson(
                response.toString(),
                TransactionResponse::class.java
            )
        )
    },
    Response.ErrorListener { error ->
        errorListener.onErrorResponse(error)
    })