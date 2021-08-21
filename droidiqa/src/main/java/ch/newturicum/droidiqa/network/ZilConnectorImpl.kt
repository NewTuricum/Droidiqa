package ch.newturicum.droidiqa.network

import android.util.Log
import ch.newturicum.droidiqa.Constants.LOG_TAG
import ch.newturicum.droidiqa.Constants.REQUEST_TAG
import ch.newturicum.droidiqa.database.dao.ZilAccountEntity
import ch.newturicum.droidiqa.dto.ZilTransaction
import ch.newturicum.droidiqa.network.request.*
import ch.newturicum.droidiqa.network.response.*
import ch.newturicum.droidiqa.security.KeyEncoder
import ch.newturicum.droidiqa.transitions.Transition
import ch.newturicum.droidiqa.util.fromBech32Address
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.VolleyError

internal class ZilConnectorImpl(private val queue: RequestQueue) : ZilliqaConnector {

    private var isAppStarted = false

    override fun getAccountBalance(
        network: ZilNetwork,
        account: ZilAccountEntity,
        responseListener: Response.Listener<AccountBalance?>,
        errorListener: Response.ErrorListener
    ) {
        if (account.address.isEmpty()) {
            errorListener.onErrorResponse(VolleyError("Empty address"))
        } else {
            addRequest(
                AccountBalanceRequest(
                    network.getRoot(),
                    accountAddress = account.address,
                    { response ->
                        responseListener.onResponse(response)
                    },
                    errorListener
                )
            )
        }
    }

    override fun getMinimumGasPrice(
        network: ZilNetwork,
        responseListener: Response.Listener<MinimumGasPrice?>,
        errorListener: Response.ErrorListener
    ) {
        addRequest(
            MinimumGasPriceRequest(
                network,
                { response ->
                    responseListener.onResponse(response)
                },
                errorListener
            )
        )
    }

    override fun getNetworkId(
        network: ZilNetwork,
        responseListener: Response.Listener<NetworkId?>,
        errorListener: Response.ErrorListener
    ) {
        addRequest(
            NetworkIdRequest(
                network,
                { response ->
                    responseListener.onResponse(response)
                },
                errorListener
            )
        )
    }

    override fun getSmartContractInit(
        network: ZilNetwork,
        contractAddress: String,
        responseListener: Response.Listener<SmartContractInit>,
        errorListener: Response.ErrorListener
    ) {
        addRequest(
            SmartContractInitRequest(
                network.getRoot(),
                arrayOf(contractAddress.fromBech32Address()),
                responseListener,
                errorListener
            )
        )
    }

    override fun getSmartContractCode(
        network: ZilNetwork,
        contractAddress: String,
        responseListener: Response.Listener<SmartContractCode>,
        errorListener: Response.ErrorListener
    ) {
        addRequest(
            SmartContractCodeRequest(
                network.getRoot(),
                contractAddress.fromBech32Address(),
                responseListener,
                errorListener
            )
        )
    }

    override fun getSmartContractStates(
        network: ZilNetwork,
        contractAddresses: List<String>,
        responseListener: Response.Listener<SmartContractState>,
        errorListener: Response.ErrorListener
    ) {
        addRequest(
            SmartContractStateRequest(
                network.getRoot(),
                contractAddresses.map { it.fromBech32Address() },
                responseListener,
                errorListener
            )
        )
    }

    override fun getTransaction(
        network: ZilNetwork,
        transactionHash: String,
        responseListener: Response.Listener<GetTransactionResponse>?,
        errorListener: Response.ErrorListener?
    ) {
        addRequest(
            GetTransactionRequest(
                network.getRoot(),
                transactionHash,
                responseListener,
                errorListener
            )
        )
    }

    override fun sendZil(
        network: ZilNetwork,
        networkId: Int,
        amount: Double,
        senderAccount: ZilAccountEntity,
        receiverAddress: String,
        gasPrice: Long,
        keyEncoder: KeyEncoder,
        responseListener: Response.Listener<ZilTransaction>,
        errorListener: Response.ErrorListener
    ) {

        val transaction = TransactionFactory.zilliqaTransfer(
            networkId, amount, senderAccount, receiverAddress, gasPrice, keyEncoder
        )
        sendTransaction(network, transaction, responseListener, errorListener, null)
    }

    override fun callSmartContractTransition(
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
    ) {
        val transaction = TransactionFactory.smartContractCall(
            networkId,
            amount,
            senderAccount,
            contractAddress,
            gasPrice,
            transition,
            keyEncoder
        )
        sendTransaction(network, transaction, responseListener, errorListener, transition)
    }

    override fun notifyAppStarted() {
        isAppStarted = true
    }

    override fun notifyAppStopped() {
        isAppStarted = false
    }

    private fun sendTransaction(
        network: ZilNetwork,
        transaction: SingleTransaction,
        responseListener: Response.Listener<ZilTransaction>,
        errorListener: Response.ErrorListener,
        transition: Transition? = null
    ) {
        addRequest(
            TransactionRequest(
                network.getRoot(),
                transaction,
                { response ->
                    if (!response.isError()) {
                        Log.d(LOG_TAG, "Transaction created: 0x${response.result?.TranID}")
                    }
                    if (transition == null) {
                        responseListener.onResponse(response.toZilTransaction(transaction))
                    } else {
                        responseListener.onResponse(
                            response.toZilTransaction(
                                transaction.toAddr,
                                transition
                            )
                        )
                    }
                },
                errorListener
            )
        )
    }

    private fun addRequest(request: Request<*>) {
        if (isAppStarted) {
            request.tag = REQUEST_TAG + "_" + request::class.java.simpleName
            Log.d(LOG_TAG, "Network: ${request.tag}")
            queue.add(request)
        }
    }

    override fun cancelAllRequests() {
        queue.cancelAll(object : RequestQueue.RequestFilter {
            override fun apply(request: Request<*>?): Boolean {
                request?.tag?.let {
                    if (it is String) {
                        return it.startsWith(REQUEST_TAG)
                    }
                }
                return false
            }
        })
    }
}