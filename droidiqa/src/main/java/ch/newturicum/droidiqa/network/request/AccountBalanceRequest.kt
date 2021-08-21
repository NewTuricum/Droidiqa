package ch.newturicum.droidiqa.network.request

import ch.newturicum.droidiqa.network.ZILLIQA
import ch.newturicum.droidiqa.network.response.AccountBalance
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.google.gson.Gson
import org.json.JSONObject


internal class AccountBalanceRequest(
    apiRoot: String,
    accountAddress: String,
    responseListener: Response.Listener<AccountBalance>,
    errorListener: Response.ErrorListener
) : JsonObjectRequest(
    Method.POST,
    apiRoot,
    JSONObject(
        Gson().toJson(
            RequestData(
                method = ZILLIQA.METHOD.ACCOUNT_BALANCE,
                params = arrayOf(accountAddress)
            )
        )
    ),
    Response.Listener { response ->
        responseListener.onResponse(
            Gson().fromJson(
                response.toString(),
                AccountBalance::class.java
            )
        )
    },
    Response.ErrorListener { error ->
        errorListener.onErrorResponse(error)
    })