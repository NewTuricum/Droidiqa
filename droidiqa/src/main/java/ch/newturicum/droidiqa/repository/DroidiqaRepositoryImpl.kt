package ch.newturicum.droidiqa.repository

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.Message
import ch.newturicum.droidiqa.Constants.DEFAULT_TRANSACTION_REFRESH_INTERVAL
import ch.newturicum.droidiqa.Constants.MIN_BALANCE_REFRESH_INTERVAL
import ch.newturicum.droidiqa.Constants.MIN_TRANSACTION_REFRESH_INTERVAL
import ch.newturicum.droidiqa.database.dao.ZilAccountEntity
import ch.newturicum.droidiqa.database.dao.ZilContactEntity
import ch.newturicum.droidiqa.database.dao.ZilTokenEntity
import ch.newturicum.droidiqa.dto.*
import ch.newturicum.droidiqa.dto.response.ZilSmartContractCode
import ch.newturicum.droidiqa.dto.response.ZilSmartContractInit
import ch.newturicum.droidiqa.dto.response.asTokenEntity
import ch.newturicum.droidiqa.network.getChainId
import ch.newturicum.droidiqa.network.response.getCode
import ch.newturicum.droidiqa.network.response.getStatus
import ch.newturicum.droidiqa.network.response.updateZilTransaction
import ch.newturicum.droidiqa.security.KeyEncoder
import ch.newturicum.droidiqa.transitions.Transition
import ch.newturicum.droidiqa.util.DroidiqaUtils
import com.android.volley.RequestQueue
import com.firestack.laksaj.crypto.KeyTools
import com.firestack.laksaj.utils.Bech32
import kotlinx.coroutines.runBlocking

abstract class DroidiqaRepositoryImpl(
    applicationContext: Context,
    keyEncoder: KeyEncoder? = null,
    volleyRequestQueue: RequestQueue? = null
) : AbstractDroidiqaRepository(applicationContext, keyEncoder, volleyRequestQueue) {

    private val contractCodes = HashMap<String, String>()

    override fun getWallet(): ZilWallet? = observables.walletLiveData.value

    override fun getTokens(): List<ZilToken>? = getWallet()?.tokens

    override fun getAccounts(): List<ZilAccount>? = observables.accountsLiveData.value

    override fun getActiveAccount(): ZilAccount? = getWallet()?.activeAccount

    override fun getMinGasPriceQa() = observables.minimumGasPrice.value ?: 0

    override fun getMinGasPriceZil() = DroidiqaUtils.qaToZil(getMinGasPriceQa())

    private val refreshHandler = RefreshHandler(Looper.getMainLooper())

    init {
        setTransactionRefreshInterval()
        setBalanceRefreshInterval()
    }

    final override fun setTransactionRefreshInterval(delayMs: Long) {
        refreshHandler.removeMessages(refreshHandler.MSG_REFRESH_PENDING_TRANSACTIONS)
        refreshHandler.transactionsUpdateIntervalMs = delayMs
        refreshHandler.requestRefreshPendingTransactions()
    }

    final override fun setBalanceRefreshInterval(delayMs: Long) {
        refreshHandler.removeMessages(refreshHandler.MSG_REFRESH_BALANCES)
        refreshHandler.balanceUpdateIntervalMs = delayMs
        refreshHandler.requestRefreshBalances()
    }

    override fun addToken(token: ZilToken): Boolean {
        currentWalletEntity()?.let { wallet ->
            val tokenEntity = ZilTokenEntity.fromZilToken(token)
            if (!wallet.containsToken(tokenEntity)) {
                wallet.addToken(tokenEntity)
                update(wallet)
                return true
            }
        }
        return false
    }

    override fun addToken(smartContractInit: ZilSmartContractInit): Boolean {
        currentWalletEntity()?.let { wallet ->
            return when {
                wallet.containsToken(smartContractInit.contractAddress) -> true
                wallet.addToken(smartContractInit.asTokenEntity()) -> {
                    update(wallet)
                    true
                }
                else -> false
            }
        }
        return false
    }

    override fun addToken(contractAddress: String, addTokenCallback: AddTokenCallback?) {
        currentWalletEntity()?.let { wallet ->
            if (!wallet.containsToken(contractAddress)) {
                DroidiqaUtils.toBech32Address(contractAddress)?.let { address ->
                    zilConnector.getSmartContractInit(
                        network = activeNetwork,
                        contractAddress = address,
                        responseListener = { response ->
                            val token = response.asZilToken()
                            wallet.addToken(token)
                            update(wallet)
                            addTokenCallback?.onSuccess(token.toZilToken())
                        },
                        errorListener = {
                            addTokenCallback?.onNetworkError()
                        }
                    )
                } ?: {
                    addTokenCallback?.onError("Invalid address: $contractAddress")
                }
            } else {
                wallet.tokens().find { it.contractAddress == contractAddress }?.toZilToken()?.let {
                    addTokenCallback?.onSuccess(it)
                } ?: {
                    // Should never happen
                    addTokenCallback?.onError("Unknown error")
                }
            }
        }
    }

    override fun removeToken(token: ZilToken) {
        currentWalletEntity()?.let {
            it.removeToken(ZilTokenEntity.fromZilToken(token))
            update(it)
        }
    }

    override fun createAccount(name: String?): ZilAccount? {
        DroidiqaUtils.generatePrivateKey().let { privateKey ->
            mKeyEncoder.encode(privateKey)?.let { encodedPrivateKey ->
                val publicKey = KeyTools.getPublicKeyFromPrivateKey(privateKey, true)
                val zilAccountEntity = ZilAccountEntity(
                    encryptedPrivateKey = encodedPrivateKey,
                    publicKey = publicKey,
                    name = name ?: getNewAccountName(),
                    address = Bech32.toBech32Address(KeyTools.getAddressFromPublicKey(publicKey)),
                    nonce = 0,
                    zilBalance = 0.0,
                    transactions = emptyList()
                )
                addAccount(zilAccountEntity)
                return zilAccountEntity.toZilAccount()
            }
        }
        return null
    }

    override fun addAccount(privateKey: String, name: String?): Boolean {
        mKeyEncoder.encode(privateKey)?.let { encodedPrivateKey ->
            val publicKey = KeyTools.getPublicKeyFromPrivateKey(privateKey, true)
            val address = DroidiqaUtils.bech32AddressFromPublicKey(publicKey) ?: ""
            val theName = name ?: getNewAccountName()
            addAccount(
                ZilAccountEntity(
                    encryptedPrivateKey = encodedPrivateKey,
                    publicKey = publicKey,
                    address = address,
                    name = theName,
                    zilBalance = 0.0,
                    nonce = 0,
                    transactions = emptyList()
                )
            )
            return true
        }
        return false
    }

    override fun removeAccount(account: ZilAccount) {
        currentWalletEntity()?.let {
            it.removeAccount(account)
            runBlocking {
                zilWalletDao.update(it)
            }
        }
    }

    override fun updateAccount(account: ZilAccount) {
        currentAccountsEntity()?.find { it.address == account.address }?.let { entity ->
            currentWalletEntity()?.let {
                it.removeAccount(account)
                it.addAccount(entity)
                runBlocking {
                    zilWalletDao.update(it)
                }
            }
        }
    }

    override fun refreshActiveAccount(callback: RefreshCallback?) {
        currentWalletEntity()?.let { wallet ->
            wallet.activeAccount()?.let { account ->
                refreshAccount(wallet, account, callback)
            } ?: run {
                callback?.onError("No active account")
            }
        } ?: run {
            callback?.onError("No wallet")
        }
    }

    override fun refreshMinimumGasPrice(callback: MinimumGasPriceCallback?) {
        zilConnector.getMinimumGasPrice(activeNetwork,
            responseListener = {
                if (it?.result == null) {
                    callback?.onError("Could not load minimum gas price")
                } else {
                    observables.minimumGasPrice.value = it.result!!.toLong()
                    callback?.onResult(getMinGasPriceQa(), getMinGasPriceZil())
                }
            },
            errorListener = {
                // Fail gracefully
                callback?.onNetworkError()
            }
        )
    }

    override fun addContact(zilContact: ZilContact) {
        runBlocking {
            zilContactDao.insertAll(ZilContactEntity.fromZilContact(zilContact))
        }
    }

    override fun deleteContact(zilContact: ZilContact) {
        runBlocking {
            zilContactDao.delete(ZilContactEntity.fromZilContact(zilContact))
        }
    }

    override fun getSmartContractInit(
        contractAddress: String,
        callback: SmartContractInitCallback?
    ) {
        zilConnector.getSmartContractInit(
            activeNetwork,
            contractAddress,
            responseListener = {
                if (it.error == null) {
                    callback?.onResult(it.asZilSmartContractInit())
                } else {
                    callback?.onError(it.error?.message ?: "")
                }
            }, errorListener = {
                callback?.onNetworkError()
            }
        )
    }

    override fun getSmartContractCode(
        contractAddress: String,
        callback: SmartContractCodeCallback?
    ) {
        contractCodes[contractAddress]?.let {
            callback?.onResult(ZilSmartContractCode(it))
            return
        }
        zilConnector.getSmartContractCode(
            activeNetwork,
            contractAddress,
            responseListener = {
                if (it.error == null) {
                    it.getCode()?.replace("/\\", "")?.let { code ->
                        contractCodes[contractAddress] = code
                        callback?.onResult(ZilSmartContractCode(code))
                    } ?: run {
                        callback?.onError("Empty code received")
                    }
                } else {
                    callback?.onError(it.error?.message ?: "")
                }
            }, errorListener = {
                callback?.onNetworkError()
            }
        )
    }

    override fun getTransactions(): List<ZilTransaction> {
        currentWalletEntity()?.activeAccount()?.let { account ->
            return account.transactions
        }
        return emptyList()
    }

    override fun addTransaction(transaction: ZilTransaction) {
        val wallet = currentWalletEntity()
        wallet?.activeAccount()?.let { account ->
            if (account.transactions.find { it.hash == transaction.hash } == null) {
                val newList = account.transactions.toMutableList()
                newList.add(transaction)
                newList.sortByDescending { it.timestamp }
                account.transactions = newList.toList()
                update(wallet)
            }
        }
    }

    override fun deleteTransaction(transaction: ZilTransaction) {
        val wallet = currentWalletEntity()
        wallet?.activeAccount()?.let { account ->
            account.transactions = account.transactions.filter { it.hash != transaction.hash }
            update(wallet)
        }
    }

    override fun findTransactionByHash(hash: String): ZilTransaction? {
        return currentWalletEntity()?.activeAccount()?.transactions?.find { it.hash == hash }
    }

    override fun refreshPendingTransactions(callback: RefreshCallback?) {
        currentWalletEntity()?.let { wallet ->
            wallet.activeAccount()?.let { account ->
                getTransactions().filter { it.status == ZilTransactionStatus.PENDING }
                    .let { pendingTransactions ->
                        if (pendingTransactions.isEmpty()) {
                            // No more pending transitions
                            callback?.onComplete(true)
                            return
                        }
                        var nUpdated = 0
                        for ((index, transaction) in pendingTransactions.withIndex()) {
                            zilConnector.getTransaction(
                                activeNetwork, transaction.hash,
                                {
                                    it?.let { response ->
                                        response.getStatus()?.let { status ->
                                            if (status == ZilTransactionStatus.COMPLETED) nUpdated++
                                            if (status != transaction.status) {
                                                it.updateZilTransaction(transaction)
                                                account.addTransaction(transaction)
                                            }
                                        }
                                    }
                                    if (index == pendingTransactions.lastIndex) {
                                        if (nUpdated > 0) {
                                            // In this case, let's also update all balances right away
                                            update(wallet)
                                            refreshActiveAccount()
                                            getPendingTransactionsCount()
                                        }
                                        callback?.onComplete(true)
                                    }
                                }, null
                            )
                        }
                    }
            } ?: kotlin.run {
                callback?.onError("No active account")
            }
        } ?: kotlin.run {
            callback?.onError("No wallet")
        }
    }

    private fun getPendingTransactionsCount(): Int {
        val count =
            currentWalletEntity()?.activeAccount()?.transactions?.filter { it.status == ZilTransactionStatus.PENDING }?.size
                ?: 0
        observables.hasPendingTransactions.value = count > 0
        return count
    }

    override fun sendZilliqa(
        amount: Double,
        receiverAddress: String,
        gasPrice: Long,
        callback: TransactionCallback?
    ) {
        refreshActiveAccount(object : RefreshCallback {
            override fun onComplete(success: Boolean) {
                executeZilSendTransaction(amount, receiverAddress, gasPrice, callback)
            }

            override fun onError(message: String) {
                callback?.onNetworkError()
            }

            override fun onNetworkError() {
                callback?.onNetworkError()
            }
        })
    }

    override fun callSmartContractTransition(
        contractAddress: String,
        gasPrice: Long,
        transition: Transition,
        callback: TransactionCallback?
    ) {
        refreshActiveAccount(object : RefreshCallback {
            override fun onComplete(success: Boolean) {
                executeSmartContractTransition(contractAddress, gasPrice, transition, callback)
            }

            override fun onError(message: String) {
                callback?.onNetworkError()
            }

            override fun onNetworkError() {
                callback?.onNetworkError()
            }
        })
    }

    private fun executeZilSendTransaction(
        amount: Double,
        receiverAddress: String,
        gasPrice: Long,
        callback: TransactionCallback?
    ) {
        val entity = currentWalletEntity()
        entity?.activeAccount()?.let { account ->
            zilConnector.sendZil(activeNetwork,
                networkIdMap[activeNetwork] ?: activeNetwork.getChainId(),
                amount,
                account,
                receiverAddress,
                gasPrice,
                mKeyEncoder,
                responseListener = { response ->
                    if (response != null) {
                        account.addTransaction(response)
                        account.nonce++
                        entity.let {
                            runBlocking {
                                zilWalletDao.update(it)
                            }
                        }
                        observables.hasPendingTransactions.value = true
                        callback?.onSuccess(response)
                    } else {
                        callback?.onError("Could not create transaction")
                    }
                },
                errorListener = {
                    callback?.onNetworkError()
                })
        } ?: run {
            callback?.onError("No active account set")
        }
    }

    private fun executeSmartContractTransition(
        contractAddress: String,
        gasPrice: Long,
        transition: Transition,
        callback: TransactionCallback?
    ) {
        val entity = currentWalletEntity()
        entity?.activeAccount()?.let { account ->
            zilConnector.callSmartContractTransition(activeNetwork,
                networkIdMap[activeNetwork] ?: activeNetwork.getChainId(),
                0.0,
                account,
                contractAddress,
                gasPrice,
                transition,
                mKeyEncoder,
                responseListener = { response ->
                    if (response != null) {
                        account.addTransaction(response)
                        account.nonce++
                        entity.let {
                            runBlocking {
                                zilWalletDao.update(it)
                            }
                        }
                        observables.hasPendingTransactions.value = true
                        callback?.onSuccess(response)
                    } else {
                        callback?.onError("Could not create transaction")
                    }
                },
                errorListener = {
                    callback?.onNetworkError()
                })
        } ?: run {
            callback?.onError("No active account set")
        }
    }

    override fun shutdown() {
        refreshHandler.removeCallbacksAndMessages(null)
        super.shutdown()
    }

    override fun start() {
        super.start()
        refreshHandler.requestRefreshPendingTransactions()
        refreshHandler.requestRefreshBalances()
    }

    private inner class RefreshHandler(looper: Looper) : Handler(looper) {
        val MSG_REFRESH_BALANCES = 0x1
        val MSG_REFRESH_PENDING_TRANSACTIONS = 0x2

        var transactionsUpdateIntervalMs = DEFAULT_TRANSACTION_REFRESH_INTERVAL
        var balanceUpdateIntervalMs = DEFAULT_TRANSACTION_REFRESH_INTERVAL

        private fun isTransactionAutoRefreshEnabled(): Boolean = transactionsUpdateIntervalMs > 0

        private fun isBalanceAutoRefreshEnabled(): Boolean = balanceUpdateIntervalMs > 0

        fun notifyBalanceUpdated() {
            if (isBalanceAutoRefreshEnabled()) {
                removeMessages(MSG_REFRESH_BALANCES)
                sendEmptyMessageDelayed(
                    MSG_REFRESH_BALANCES,
                    balanceUpdateIntervalMs
                )
            }
        }

        fun requestRefreshPendingTransactions(
            delayMs: Long = transactionsUpdateIntervalMs.coerceAtLeast(
                MIN_TRANSACTION_REFRESH_INTERVAL
            )
        ) {
            transactionsUpdateIntervalMs = delayMs.coerceAtLeast(MIN_TRANSACTION_REFRESH_INTERVAL)
            removeMessages(MSG_REFRESH_PENDING_TRANSACTIONS)
            if (isTransactionAutoRefreshEnabled()) {
                sendEmptyMessageDelayed(
                    MSG_REFRESH_PENDING_TRANSACTIONS,
                    transactionsUpdateIntervalMs
                )
            }
        }

        fun requestRefreshBalances(
            delayMs: Long = balanceUpdateIntervalMs.coerceAtLeast(MIN_BALANCE_REFRESH_INTERVAL)
        ) {
            balanceUpdateIntervalMs = delayMs.coerceAtLeast(MIN_BALANCE_REFRESH_INTERVAL)
            removeMessages(MSG_REFRESH_BALANCES)
            if (isBalanceAutoRefreshEnabled()) {
                sendEmptyMessageDelayed(MSG_REFRESH_BALANCES, balanceUpdateIntervalMs)
            }
        }

        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
            when (msg.what) {
                MSG_REFRESH_BALANCES -> {
                    removeMessages(MSG_REFRESH_BALANCES)
                    refreshActiveAccount(object : RefreshCallback {
                        override fun onComplete(success: Boolean) {
                            if (isBalanceAutoRefreshEnabled())
                                requestRefreshBalances()
                        }

                        override fun onError(message: String) {
                            if (isBalanceAutoRefreshEnabled())
                                requestRefreshBalances()
                        }

                        override fun onNetworkError() {
                            if (isBalanceAutoRefreshEnabled())
                                requestRefreshBalances()
                        }
                    })
                }
                MSG_REFRESH_PENDING_TRANSACTIONS -> {
                    removeMessages(MSG_REFRESH_PENDING_TRANSACTIONS)
                    if (getPendingTransactionsCount() > 0) {
                        refreshPendingTransactions(object : RefreshCallback {
                            override fun onComplete(success: Boolean) {
                                if (isTransactionAutoRefreshEnabled())
                                    requestRefreshPendingTransactions()
                            }

                            override fun onError(message: String) {
                                if (isTransactionAutoRefreshEnabled())
                                    requestRefreshPendingTransactions()
                            }

                            override fun onNetworkError() {
                                if (isTransactionAutoRefreshEnabled())
                                    requestRefreshPendingTransactions()
                            }
                        })
                    } else {
                        if (isTransactionAutoRefreshEnabled())
                            requestRefreshPendingTransactions()
                    }
                }
            }
        }
    }
}