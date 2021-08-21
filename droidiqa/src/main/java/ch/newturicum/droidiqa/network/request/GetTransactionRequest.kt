package ch.newturicum.droidiqa.network.request

import ch.newturicum.droidiqa.network.ZILLIQA
import ch.newturicum.droidiqa.network.response.GetTransactionResponse
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.google.gson.Gson
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
        Gson().toJson(
            RequestData(
                method = ZILLIQA.METHOD.GET_TRANSACTION,
                params = arrayOf(transactionId)
            )
        )
    ),
    Response.Listener { response ->
        responseListener?.onResponse(
            Gson().fromJson(
                response.toString(),
                GetTransactionResponse::class.java
            )
        )
    },
    Response.ErrorListener { error ->
        errorListener?.onErrorResponse(error)
    })