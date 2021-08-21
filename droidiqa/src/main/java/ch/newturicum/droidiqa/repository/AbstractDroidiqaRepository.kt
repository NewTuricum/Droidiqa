package ch.newturicum.droidiqa.repository

import android.content.Context
import android.util.Log
import androidx.annotation.WorkerThread
import androidx.lifecycle.*
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import ch.newturicum.droidiqa.Constants
import ch.newturicum.droidiqa.database.ZilDroidDatabase
import ch.newturicum.droidiqa.database.dao.AccountContainer
import ch.newturicum.droidiqa.database.dao.TokenContainer
import ch.newturicum.droidiqa.database.dao.ZilAccountEntity
import ch.newturicum.droidiqa.database.dao.ZilWalletEntity
import ch.newturicum.droidiqa.dto.ZilAccount
import ch.newturicum.droidiqa.dto.ZilContact
import ch.newturicum.droidiqa.dto.ZilToken
import ch.newturicum.droidiqa.dto.ZilWallet
import ch.newturicum.droidiqa.network.ZilConnectorImpl
import ch.newturicum.droidiqa.network.ZilNetwork
import ch.newturicum.droidiqa.network.ZilliqaConnector
import ch.newturicum.droidiqa.network.getChainId
import ch.newturicum.droidiqa.security.KeyEncoder
import ch.newturicum.droidiqa.security.KeyEncoderImpl
import ch.newturicum.droidiqa.util.DroidiqaLifecycleProvider
import ch.newturicum.droidiqa.util.DroidiqaUtils
import com.android.volley.RequestQueue
import com.android.volley.toolbox.Volley
import kotlinx.coroutines.runBlocking

/**
 * Base repository that implements internal helpers and data structures.
 */
abstract class AbstractDroidiqaRepository(
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

    internal val mKeyEncoder = keyEncoder ?: KeyEncoderImpl()
    private val context = applicationContext.applicationContext
    private val sharedPreferences =
        context.getSharedPreferences(PREF_NETWORK, Context.MODE_PRIVATE)
    internal val zilConnector: ZilliqaConnector =
        ZilConnectorImpl(volleyRequestQueue ?: Volley.newRequestQueue(context))
    internal val networkIdMap = HashMap<ZilNetwork, Int>()

    internal var activeNetwork: ZilNetwork =
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
    internal val zilWalletDao = database.zilWalletDao()
    internal val zilContactDao = database.zilContactDao()
    private var appState = AppState.STOPPED

    class Observables {
        /**
         * Observable async loading state. This will be <code>true</code> while the client is carrying out
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
         * Observable data of the wallet at currently set <code>activeNetwork</code>.
         */
        lateinit var walletLiveData: LiveData<ZilWallet>

        /**
         * Observable data of the presence of pending transactions.
         */
        val hasPendingTransactions = MutableLiveData<Boolean>()

        /**
         * Observable data of the wallet's accounts at currently set <code>activeNetwork</code>.
         */
        lateinit var accountsLiveData: LiveData<List<ZilAccount>>

        /**
         * Observable data of the wallet's tokens at currently set <code>activeNetwork</code>.
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

    internal fun update(wallet: ZilWalletEntity) {
        runBlocking {
            zilWalletDao.update(wallet)
        }
    }

    private lateinit var walletEntity: LiveData<ZilWalletEntity>
    private lateinit var accountsEntity: LiveData<List<ZilAccountEntity>>


    protected fun setLoading(loading: Boolean) {
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

    internal fun getNewAccountName(): String =
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
        zilConnector.cancelAllRequests()
        database.close()
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    protected open fun start() {
        Log.d(Constants.LOG_TAG, "State change: App started")
        zilConnector.notifyAppStarted()
        appState = AppState.STARTED
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    protected fun stop() {
        Log.d(Constants.LOG_TAG, "State change: App stopped")
        zilConnector.notifyAppStopped()
        appState = AppState.STOPPED
    }

    protected fun isAppStarted(): Boolean = appState == AppState.STARTED

    internal open fun onDatabaseReady() {
        setObservers()
        refreshMinimumGasPrice()
    }
}