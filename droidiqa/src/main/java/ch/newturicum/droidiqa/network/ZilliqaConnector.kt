package ch.newturicum.droidiqa.network

import ch.newturicum.droidiqa.database.dao.ZilAccountEntity
import ch.newturicum.droidiqa.dto.ZilTransaction
import ch.newturicum.droidiqa.network.response.*
import ch.newturicum.droidiqa.security.KeyEncoder
import ch.newturicum.droidiqa.transitions.Transition
import com.android.volley.Response

internal interface ZilliqaConnector {

    fun getAccountBalance(
        network: ZilNetwork,
        account: ZilAccountEntity,
        responseListener: Response.Listener<AccountBalance?>,
        errorListener: Response.ErrorListener
    )

    fun getMinimumGasPrice(
        network: ZilNetwork,
        responseListener: Response.Listener<MinimumGasPrice?>,
        errorListener: Response.ErrorListener
    )

    fun getNetworkId(
        network: ZilNetwork,
        responseListener: Response.Listener<NetworkId?>,
        errorListener: Response.ErrorListener
    )

    fun getSmartContractInit(
        network: ZilNetwork,
        contractAddress: String,
        responseListener: Response.Listener<SmartContractInit>,
        errorListener: Response.ErrorListener
    )

    fun getSmartContractCode(
        network: ZilNetwork,
        contractAddress: String,
        responseListener: Response.Listener<SmartContractCode>,
        errorListener: Response.ErrorListener
    )

    fun getSmartContractStates(
        network: ZilNetwork,
        contractAddresses: List<String>,
        responseListener: Response.Listener<SmartContractState>,
        errorListener: Response.ErrorListener
    )

    fun getTransaction(
        network: ZilNetwork,
        transactionId: String,
        responseListener: Response.Listener<GetTransactionResponse>?,
        errorListener: Response.ErrorListener?
    )

    fun sendZil(
        network: ZilNetwork,
        networkId: Int,
        amount: Double,
        senderAccount: ZilAccountEntity,
        receiverAddress: String,
        gasPrice: Long,
        keyEncoder: KeyEncoder,
        responseListener: Response.Listener<ZilTransaction>,
        errorListener: Response.ErrorListener
    )

    fun callSmartContractTransition(
        network: ZilNetwork,
        networkId: Int,
        amount: Double,
        senderAccount: ZilAccountEntity,
        contractAddress: String,
        gasPrice: Long,
        transition: Transition,
        keyEncoder: KeyEncoder,
        responseListener: Response.Listener<ZilTransaction>,
        errorListener: Response.ErrorListener
    )

    fun notifyAppStarted()

    fun notifyAppStopped()

    fun cancelAllRequests()
}