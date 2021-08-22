package ch.newturicum.droidiqa

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.Log
import androidx.annotation.WorkerThread
import androidx.lifecycle.*
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import ch.newturicum.droidiqa.Constants.DEFAULT_TRANSACTION_REFRESH_INTERVAL
import ch.newturicum.droidiqa.Constants.MIN_BALANCE_REFRESH_INTERVAL
import ch.newturicum.droidiqa.Constants.MIN_TRANSACTION_REFRESH_INTERVAL
import ch.newturicum.droidiqa.database.ZilDroidDatabase
import ch.newturicum.droidiqa.database.dao.*
import ch.newturicum.droidiqa.database.dao.AccountContainer
import ch.newturicum.droidiqa.database.dao.TokenContainer
import ch.newturicum.droidiqa.database.dao.ZilAccountEntity
import ch.newturicum.droidiqa.database.dao.ZilContactEntity
import ch.newturicum.droidiqa.database.dao.ZilTokenEntity
import ch.newturicum.droidiqa.database.dao.ZilWalletEntity
import ch.newturicum.droidiqa.dto.*
import ch.newturicum.droidiqa.dto.response.ZilSmartContractCode
import ch.newturicum.droidiqa.dto.response.ZilSmartContractInit
import ch.newturicum.droidiqa.dto.response.asTokenEntity
import ch.newturicum.droidiqa.network.ZilConnectorImpl
import ch.newturicum.droidiqa.network.ZilNetwork
import ch.newturicum.droidiqa.network.ZilliqaConnector
import ch.newturicum.droidiqa.network.getChainId
import ch.newturicum.droidiqa.network.response.getCode
import ch.newturicum.droidiqa.network.response.getStatus
import ch.newturicum.droidiqa.network.response.updateZilTransaction
import ch.newturicum.droidiqa.repository.*
import ch.newturicum.droidiqa.security.KeyEncoder
import ch.newturicum.droidiqa.security.KeyEncoderImpl
import ch.newturicum.droidiqa.transitions.Transition
import ch.newturicum.droidiqa.util.DroidiqaLifecycleProvider
import ch.newturicum.droidiqa.util.DroidiqaUtils
import com.android.volley.RequestQueue
import com.android.volley.toolbox.Volley
import com.firestack.laksaj.crypto.KeyTools
import com.firestack.laksaj.utils.Bech32
import kotlinx.coroutines.runBlocking

/**
 * The Client repository with access to the Zilliqa blockchain. It is recommended to instantiate this
 * as a singleton.
 * @param applicationContext The application context.
 * @param keyEncoder Optional - A customized implementation used to encode / decode private keys locally.
 * The default implementation uses a symmetric key encryption via the local Android keystore.
 * @param volleyRequestQueue Optional - If you want to integrate the client with your already existing
 * request queue, you can pass it here (default: The client maintains its own request queue and Droidiqa
 * will manage its request lifetimes internally).
 */
class Droidiqa constructor (
    applicationContext: Context,
    keyEncoder: KeyEncoder? = null,
    volleyRequestQueue: RequestQueue? = null
) : DroidiqaRepository, LifecycleObserver {

    companion object {
        const val PREF_NETWORK = "ZilDroidPreferences"
    }

    internal enum class AppState {
        STARTED,
        STOPPED
    }

    private val contractCodes = HashMap<String, String>()

    override fun getWallet(): ZilWallet? = observables.walletLiveData.value

    override fun getTokens(): List<ZilToken>? = getWallet()?.tokens

    override fun getAccounts(): List<ZilAccount>? = observables.accountsLiveData.value

    override fun getActiveAccount(): ZilAccount? = getWallet()?.activeAccount

    override fun getMinGasPriceQa() = observables.minimumGasPrice.value ?: 0

    override fun getMinGasPriceZil() = DroidiqaUtils.qaToZil(getMinGasPriceQa())

    private val refreshHandler = RefreshHandler(Looper.getMainLooper())

    private val mKeyEncoder = keyEncoder ?: KeyEncoderImpl()
    private val context = applicationContext.applicationContext
    private val sharedPreferences =
        context.getSharedPreferences(PREF_NETWORK, Context.MODE_PRIVATE)
    private val zilConnector: ZilliqaConnector =
        ZilConnectorImpl(volleyRequestQueue ?: Volley.newRequestQueue(context))
    private val networkIdMap = HashMap<ZilNetwork, Int>()

    private var activeNetwork: ZilNetwork =
        ZilNetwork.valueOf(sharedPreferences.getString(PREF_NETWORK, ZilNetwork.MAIN.name)!!)
        set(value) {
            field = value
            sharedPreferences.edit().putString(PREF_NETWORK, value.name).apply()
            runBlocking {
                // Safety check if wallet exists:
                if (zilWalletDao.getDataFor(activeNetwork) == null) {
                    insert(
                        ZilWalletEntity(
                            activeNetwork,
                            null,
                            AccountContainer(mutableListOf()),
                            TokenContainer(mutableListOf())
                        )
                    )
                }
                setObservers()
                refreshNetworkId()
                refreshMinimumGasPrice()
            }
        }

    private val database: ZilDroidDatabase =
        ZilDroidDatabase.getInstance(
            context,
            Constants.DEFAULT_DATABASE_NAME,
            mKeyEncoder,
            zilConnector,
            activeNetwork,
            object : RoomDatabase.Callback() {
                override fun onOpen(db: SupportSQLiteDatabase) {
                    super.onOpen(db)
                    onDatabaseReady()
                }
            }
        )
    private val zilWalletDao = database.zilWalletDao()
    private val zilContactDao = database.zilContactDao()
    private var appState = AppState.STOPPED

    class Observables {
        /**
         * Observable async loading state. This will be true while the client is carrying out
         * network access of any kind.
         */
        val isLoading = MutableLiveData<Boolean>().apply {
            value = false
        }

        /**
         * Observable data of the most recently known minimum gas price.
         */
        val minimumGasPrice: MutableLiveData<Long> = MutableLiveData()

        /**
         * Observable data of the wallet at currently set activeNetwork.
         */
        lateinit var walletLiveData: LiveData<ZilWallet>

        /**
         * Observable data of the presence of pending transactions.
         */
        val hasPendingTransactions = MutableLiveData<Boolean>()

        /**
         * Observable data of the wallet's accounts at currently set activeNetwork.
         */
        lateinit var accountsLiveData: LiveData<List<ZilAccount>>

        /**
         * Observable data of the wallet's tokens at currently set activeNetwork.
         */
        lateinit var tokensLiveData: LiveData<List<ZilToken>>

        /**
         * Observable data of all known contacts.
         */
        lateinit var contacts: LiveData<List<ZilContact>>
    }

    /**
     * Contains various observable LiveData fields of internal Droidiqa states.
     */
    val observables = Observables()

    init {
        require(context is DroidiqaLifecycleProvider) { "Context must implement DroidiqaLifecycleProvider interface" }
        this.let {
            context.getProcessLifecycleOwner().lifecycle.addObserver(it)
        }
        networkIdMap.apply {
            // Initialize with default hardcoded ids
            for (network in ZilNetwork.values()) {
                this[network] = network.getChainId()
            }
        }
        refreshNetworkId()
        runBlocking {
            if (zilWalletDao.getDataFor(activeNetwork) == null) {
                insert(
                    ZilWalletEntity(
                        activeNetwork, null, AccountContainer(mutableListOf()),
                        TokenContainer(mutableListOf())
                    )
                )
            }
        }
        setTransactionRefreshInterval()
        setBalanceRefreshInterval()
    }

    override fun setTransactionRefreshInterval(delayMs: Long) {
        refreshHandler.removeMessages(refreshHandler.MSG_REFRESH_PENDING_TRANSACTIONS)
        refreshHandler.transactionsUpdateIntervalMs = delayMs
        refreshHandler.requestRefreshPendingTransactions()
    }

    override fun setBalanceRefreshInterval(delayMs: Long) {
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
        callback: TransitionCallback?
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
        callback: TransitionCallback?
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
        callback: TransitionCallback?
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
        callback: TransitionCallback?
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

    @Suppress("RedundantSuspendModifier")
    @WorkerThread
    internal suspend fun insert(wallet: ZilWalletEntity) {
        zilWalletDao.insert(wallet)
    }

    @Suppress("RedundantSuspendModifier")
    @WorkerThread
    internal suspend fun delete(wallet: ZilWalletEntity) {
        zilWalletDao.delete(wallet)
    }

    private fun update(wallet: ZilWalletEntity) {
        runBlocking {
            zilWalletDao.update(wallet)
        }
    }

    private lateinit var walletEntity: LiveData<ZilWalletEntity>
    private lateinit var accountsEntity: LiveData<List<ZilAccountEntity>>


    private fun setLoading(loading: Boolean) {
        if (loading != observables.isLoading.value) {
            observables.isLoading.value = loading
        }
    }

    private fun isLoading(): Boolean = observables.isLoading.value == true

    private fun setObservers() {
        walletEntity = zilWalletDao.getFor(activeNetwork)
        accountsEntity = Transformations.map(zilWalletDao.getAccounts(activeNetwork)) {
            it.entries
        }

        observables.walletLiveData = Transformations.map(walletEntity) {
            it.toZilWallet()
        }
        observables.accountsLiveData =
            Transformations.map(accountsEntity) {
                it.map { zilAccountEntity -> zilAccountEntity.toZilAccount() }
            }
        observables.tokensLiveData =
            Transformations.map(zilWalletDao.getTokens(activeNetwork)) {
                it.entries.map { entry -> entry.toZilToken() }
            }
        observables.contacts = Transformations.map(zilContactDao.getAll()) {
            it.map { entry -> entry.toZilContact() }
        }
    }

    internal fun currentWalletEntity(): ZilWalletEntity? = walletEntity.value
    internal fun currentAccountsEntity(): List<ZilAccountEntity>? = accountsEntity.value

    internal fun addAccount(account: ZilAccountEntity, callback: RefreshCallback? = null) {
        currentWalletEntity()?.let {
            it.addAccount(account)
            update(it)
            refreshAccount(it, account, callback)
        }
    }

    override fun setActiveAccount(account: ZilAccount): Boolean {
        currentWalletEntity()?.let {
            if (it.activeAccountAddress != account.address && it.containsAccount(account.address)) {
                it.activeAccountAddress = account.address
                return runBlocking {
                    zilWalletDao.update(it)
                    refreshActiveAccount()
                    return@runBlocking true
                }
            }
        }
        return false
    }

    override fun findToken(contractAddress: String): ZilToken? {
        val address = DroidiqaUtils.toBech32Address(contractAddress)
        return currentWalletEntity()?.tokens()?.find { it.contractAddress == address }?.toZilToken()
    }

    override fun findContact(address: String): ZilContact? {
        val addressBech32 = DroidiqaUtils.toBech32Address(address)
        val contacts = observables.contacts.value
        return contacts?.find { it.address == addressBech32 }
    }

    override fun getPrivateKey(account: ZilAccount): String? {
        currentWalletEntity()?.let { wallet ->
            val entity = wallet.accounts().find { it.address == account.address }
            return if (entity == null) null else mKeyEncoder.decode(entity.encryptedPrivateKey)
        }
        return null
    }

    override fun getAccountCount(): Int {
        return currentWalletEntity()?.accounts()?.size ?: 0
    }

    internal fun refreshAccount(
        wallet: ZilWalletEntity,
        account: ZilAccountEntity,
        callback: RefreshCallback? = null
    ) {
        wallet.findAccount(accountAddress = account.address)?.let { knownAccount ->
            setLoading(true)
            zilConnector.getAccountBalance(
                network = wallet.network,
                account = knownAccount,
                responseListener = { accountBalance ->
                    accountBalance?.let {
                        accountBalance.result?.let { result ->
                            knownAccount.zilBalance = result.balance.toDouble()
                            knownAccount.nonce = result.nonce
                        }
                    }
                    refreshTokenStates(wallet)
                    runBlocking {
                        zilWalletDao.update(wallet)
                    }
                    callback?.onComplete(true)
                    setLoading(false)
                }, errorListener = {
                    // Fail cracefully
                    callback?.onComplete(false)
                    setLoading(false)
                }
            )
        } ?: run {
            callback?.onError("Account not found in db.")
        }
    }

    private fun refreshTokenStates(
        wallet: ZilWalletEntity,
        callback: RefreshCallback? = null
    ) {
        val tokenList = wallet.tokens()
        for ((index, token) in tokenList.withIndex()) {
            setLoading(true)
            zilConnector.getSmartContractStates(
                network = wallet.network,
                contractAddresses = listOf(token.contractAddress),
                responseListener = { response ->
                    response.result?.balances?.let {
                        token.balances = it
                    }
                    runBlocking {
                        zilWalletDao.update(wallet)
                    }
                    if (index == tokenList.lastIndex) {
                        callback?.onComplete(true)
                        setLoading(false)
                    }
                },
                errorListener = {
                    // Fail cracefully
                    if (index == tokenList.lastIndex) {
                        callback?.onComplete(false)
                        setLoading(false)
                    }
                }
            )
        }
    }

    private fun getNewAccountName(): String =
        "Account ${currentWalletEntity()?.accountCount() ?: 0}"

    internal fun refreshAllAccounts(wallet: ZilWalletEntity) {
        for (account in wallet.accounts()) {
            refreshAccount(wallet, account)
        }
    }

    private fun refreshNetworkId(network: ZilNetwork = activeNetwork) {
        zilConnector.getNetworkId(network,
            responseListener = { response ->
                response?.result?.let { id ->
                    networkIdMap[network] = id.toInt()
                }
            }, errorListener = {
                // Do nothing
            })
    }

    override fun setNetwork(network: ZilNetwork) {
        if (network != activeNetwork) {
            activeNetwork = network
            setObservers()
        }
    }

    override fun getNetwork(): ZilNetwork = activeNetwork

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    override fun shutdown() {
        Log.d(Constants.LOG_TAG, "State change: App terminated")
        refreshHandler.removeCallbacksAndMessages(null)
        zilConnector.cancelAllRequests()
        database.close()
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    private fun start() {
        Log.d(Constants.LOG_TAG, "State change: App started")
        zilConnector.notifyAppStarted()
        appState = AppState.STARTED
        refreshHandler.requestRefreshPendingTransactions()
        refreshHandler.requestRefreshBalances()
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    private fun stop() {
        Log.d(Constants.LOG_TAG, "State change: App stopped")
        zilConnector.notifyAppStopped()
        appState = AppState.STOPPED
    }

    private fun isAppStarted(): Boolean = appState == AppState.STARTED

    internal fun onDatabaseReady() {
        setObservers()
        refreshMinimumGasPrice()
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