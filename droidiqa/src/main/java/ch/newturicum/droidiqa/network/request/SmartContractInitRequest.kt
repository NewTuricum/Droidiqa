package ch.newturicum.droidiqa.network.request

import ch.newturicum.droidiqa.network.ZILLIQA
import ch.newturicum.droidiqa.network.response.SmartContractInit
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.json.JSONObject

internal class SmartContractInitRequest(
    apiRoot: String,
    contractAddress: Array<String>,
    responseListener: Response.Listener<SmartContractInit>,
    errorListener: Response.ErrorListener
) : JsonObjectRequest(
    Method.POST,
    apiRoot,
    JSONObject(
        Json.encodeToString(
            RequestData(
                method = ZILLIQA.METHOD.SMARTCONTRACT_INIT,
                params = contractAddress
            )
        )
    ),
    Response.Listener { response ->
        responseListener.onResponse(
            Json.decodeFromString<SmartContractInit>(response.toString())
        )
    },
    Response.ErrorListener { error ->
        errorListener.onErrorResponse(error)
    }
)