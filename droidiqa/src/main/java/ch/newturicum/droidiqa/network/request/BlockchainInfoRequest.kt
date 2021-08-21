package ch.newturicum.droidiqa.network.request

import ch.newturicum.droidiqa.network.ZILLIQA
import ch.newturicum.droidiqa.network.response.BlockchainInfo
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.google.gson.Gson
import org.json.JSONObject


internal class BlockchainInfoRequest(
    apiRoot: String,
    responseListener: Response.Listener<BlockchainInfo>,
    errorListener: Response.ErrorListener
) : JsonObjectRequest(
    Method.POST,
    apiRoot,
    JSONObject(Gson().toJson(RequestData(method = ZILLIQA.METHOD.BLOCKCHAIN_INFO))),
    Response.Listener { response ->
        responseListener.onResponse(
            Gson().fromJson(
                response.toString(),
                BlockchainInfo::class.java
            )
        )
    },
    Response.ErrorListener { error ->
        errorListener.onErrorResponse(error)
    }) {
}