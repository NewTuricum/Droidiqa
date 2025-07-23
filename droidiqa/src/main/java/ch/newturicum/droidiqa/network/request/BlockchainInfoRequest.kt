package ch.newturicum.droidiqa.network.request

import ch.newturicum.droidiqa.network.ZILLIQA
import ch.newturicum.droidiqa.network.response.BlockchainInfo
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.json.JSONObject

internal class BlockchainInfoRequest(
    apiRoot: String,
    responseListener: Response.Listener<BlockchainInfo>,
    errorListener: Response.ErrorListener
) : JsonObjectRequest(
    Method.POST,
    apiRoot,
    JSONObject(
        Json.encodeToString(RequestData(method = ZILLIQA.METHOD.BLOCKCHAIN_INFO))
    ),
    Response.Listener { response ->
        responseListener.onResponse(
            Json.decodeFromString<BlockchainInfo>(response.toString())
        )
    },
    Response.ErrorListener { error ->
        errorListener.onErrorResponse(error)
    }
)