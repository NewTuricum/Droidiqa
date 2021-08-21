package ch.newturicum.droidiqa.network.request

import ch.newturicum.droidiqa.network.ZILLIQA
import ch.newturicum.droidiqa.network.response.SmartContractInit
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.google.gson.Gson
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
        Gson().toJson(
            RequestData(
                method = ZILLIQA.METHOD.SMARTCONTRACT_INIT,
                params = contractAddress
            )
        )
    ),
    Response.Listener { response ->
        responseListener.onResponse(
            Gson().fromJson(
                response.toString(),
                SmartContractInit::class.java
            )
        )
    },
    Response.ErrorListener { error ->
        errorListener.onErrorResponse(error)
    })