package ch.newturicum.droidiqa.network.request

import ch.newturicum.droidiqa.network.ZILLIQA
import ch.newturicum.droidiqa.network.ZilNetwork
import ch.newturicum.droidiqa.network.getRoot
import ch.newturicum.droidiqa.network.response.NetworkId
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.google.gson.Gson
import org.json.JSONObject


internal class NetworkIdRequest(
    network: ZilNetwork,
    responseListener: Response.Listener<NetworkId>,
    errorListener: Response.ErrorListener
) : JsonObjectRequest(
    Method.POST,
    network.getRoot(),
    JSONObject(
        Gson().toJson(
            RequestData(
                method = ZILLIQA.METHOD.NETWORK_ID,
                params = arrayOf()
            )
        )
    ),
    Response.Listener { response ->
        responseListener.onResponse(
            Gson().fromJson(
                response.toString(),
                NetworkId::class.java
            )
        )
    },
    Response.ErrorListener { error ->
        errorListener.onErrorResponse(error)
    })