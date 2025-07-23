package ch.newturicum.droidiqa.network.request

import ch.newturicum.droidiqa.network.ZILLIQA
import ch.newturicum.droidiqa.network.response.GetTransactionResponse
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.json.JSONObject

internal class GetTransactionRequest(
    apiRoot: String,
    transactionId: String,
    responseListener: Response.Listener<GetTransactionResponse>?,
    errorListener: Response.ErrorListener?
) : JsonObjectRequest(
    Method.POST,
    apiRoot,
    JSONObject(
        Json.encodeToString(
            RequestData(
                method = ZILLIQA.METHOD.GET_TRANSACTION,
                params = arrayOf(transactionId)
            )
        )
    ),
    Response.Listener { response ->
        responseListener?.onResponse(
            Json.decodeFromString<GetTransactionResponse>(response.toString())
        )
    },
    Response.ErrorListener { error ->
        errorListener?.onErrorResponse(error)
    }
)