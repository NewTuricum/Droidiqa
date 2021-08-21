package ch.newturicum.droidiqa.network.request

import ch.newturicum.droidiqa.network.ZILLIQA
import ch.newturicum.droidiqa.network.response.SmartContractState
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.google.gson.Gson
import org.json.JSONObject


internal class SmartContractStateRequest(
    apiRoot: String,
    contractAddresses: List<String>,
    responseListener: Response.Listener<SmartContractState>,
    errorListener: Response.ErrorListener
) : JsonObjectRequest(
    Method.POST,
    apiRoot,
    JSONObject(
        Gson().toJson(
            RequestData(
                method = ZILLIQA.METHOD.SMARTCONTRACT_STATE,
                params = contractAddresses.toTypedArray()
            )
        )
    ),
    Response.Listener { response ->
        responseListener.onResponse(
            Gson().fromJson(
                response.toString(),
                SmartContractState::class.java
            )
        )
    },
    Response.ErrorListener { error ->
        errorListener.onErrorResponse(error)
    })