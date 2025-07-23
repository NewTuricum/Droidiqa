package ch.newturicum.droidiqa.network.request

import ch.newturicum.droidiqa.network.ZILLIQA
import ch.newturicum.droidiqa.network.ZilNetwork
import ch.newturicum.droidiqa.network.getRoot
import ch.newturicum.droidiqa.network.response.NetworkId
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.json.JSONObject

internal class NetworkIdRequest(
    network: ZilNetwork,
    responseListener: Response.Listener<NetworkId>,
    errorListener: Response.ErrorListener
) : JsonObjectRequest(
    Method.POST,
    network.getRoot(),
    JSONObject(
        Json.encodeToString(
            RequestData(
                method = ZILLIQA.METHOD.NETWORK_ID,
                params = arrayOf()
            )
        )
    ),
    Response.Listener { response ->
        responseListener.onResponse(
            Json.decodeFromString<NetworkId>(response.toString())
        )
    },
    Response.ErrorListener { error ->
        errorListener.onErrorResponse(error)
    }
)