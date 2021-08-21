package ch.newturicum.droidiqa.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import ch.newturicum.droidiqa.database.dao.*
import ch.newturicum.droidiqa.network.ZilNetwork
import ch.newturicum.droidiqa.network.ZilliqaConnector
import ch.newturicum.droidiqa.security.KeyEncoder
import com.firestack.laksaj.crypto.KeyTools
import kotlinx.coroutines.runBlocking

@Database(
    entities = [ZilContactEntity::class, ZilAccountEntity::class, ZilTokenEntity::class, ZilWalletEntity::class],
    version = 2
)
@TypeConverters(Converters::class)
internal abstract class ZilDroidDatabase : RoomDatabase() {

    companion object {
        fun getInstance(
            applicationContext: Context,
            name: String,
            keyEncoder: KeyEncoder,
            zilConnector: ZilliqaConnector,
            network: ZilNetwork,
            callback: RoomDatabase.Callback?
        ): ZilDroidDatabase {
            require(name.isNotEmpty())
            val dbBuilder = Room.databaseBuilder(
                applicationContext.applicationContext,
                ZilDroidDatabase::class.java, name
            )
            callback?.let {
                dbBuilder.addCallback(it)
            }
            dbBuilder.build().apply {
                initialize(keyEncoder, zilConnector, network)
                return this
            }
        }
    }

    abstract fun zilContactDao(): ZilContactDao
    abstract fun zilWalletDao(): ZilWalletDao

    internal var keyEncoder: KeyEncoder? = null
    private lateinit var zilConnector: ZilliqaConnector
    var network: ZilNetwork = ZilNetwork.MAIN
        set(value) {
            field = value
            // TODO: Refresh wallet from BC
        }

    fun initialize(
        keyEncoder: KeyEncoder,
        zilConnector: ZilliqaConnector,
        network: ZilNetwork
    ) {
        this.keyEncoder = keyEncoder
        this.zilConnector = zilConnector

        runBlocking {
            for (networkType in ZilNetwork.values()) {
                if (zilWalletDao().getFor(networkType) == null) {
                    walletNew(networkType)
                }
            }
        }
        this.network = network
    }

    suspend fun refreshWalletBalance(wallet: ZilWalletEntity) {
        for (account in wallet.accounts()) {
            zilConnector.getAccountBalance(
                network = wallet.network,
                account = account,
                responseListener = { accountBalance ->
                    accountBalance?.let {
                        accountBalance.result?.let { result ->
                            account.zilBalance = result.balance.toDouble()
                            account.nonce = result.nonce
                        }
                    }
                    runBlocking {
                        zilWalletDao().update(wallet)
                    }
                }, errorListener = {
                    // Fail cracefully
                }
            )
        }
    }

    // region WALLET

    suspend fun walletNew(network: ZilNetwork): ZilWalletEntity {
        return ZilWalletEntity(
            network = network,
            _tokenContainer = TokenContainer(mutableListOf()),
            _accountContainer = AccountContainer(mutableListOf()),
            activeAccountAddress = null
        ).apply {
            zilWalletDao().create(this)
        }
    }

    fun removeTokenFromAllWallets(token: ZilTokenEntity) {
        // TODO
        /*
        runBlocking {
            for (wallet in zilWalletDao().getAll()) {
                if (wallet.containsToken(token)) {
                    wallet.removeToken(token)
                    for (account in wallet.accounts) {
                        account.removeTokenBalance(token)
                    }
                    zilWalletDao().update(wallet)
                }
            }
        }*/
    }

    fun walletAddOrRefreshToken(wallet: ZilWalletEntity, newToken: ZilTokenEntity) {
        runBlocking {
            if (!wallet.containsToken(newToken)) {
                wallet.addToken(newToken)
                zilWalletDao().update(wallet)
            }
        }
    }

    fun walletSetActiveAccount(wallet: ZilWalletEntity, account: ZilAccountEntity) {
        runBlocking {
            wallet.activeAccountAddress = account.address
            if (wallet.containsAccount(account)) {
                wallet.activeAccountAddress = account.address
            }
            zilWalletDao().update(wallet)
        }
    }

    fun walletAccountDelete(wallet: ZilWalletEntity, account: ZilAccountEntity) {
        runBlocking {
            wallet.setAccounts(wallet.accounts().filter { it.address != account.address })
            zilWalletDao().update(wallet)
        }
    }

    fun walletAccountCreate(
        wallet: ZilWalletEntity,
        privateKey: String,
        name: String? = null
    ): ZilAccountEntity? {
        require(keyEncoder != null && privateKey.isNotEmpty())
        return runBlocking {
            val accountName: String = name ?: "Account ${wallet.accounts().size}"
            keyEncoder?.let { crypto ->
                val publicKey = KeyTools.getPublicKeyFromPrivateKey(privateKey, true)
                val address = KeyTools.getAddressFromPublicKey(publicKey)
                wallet.findAccount(address)?.let { exisitingAccount ->
                    return@runBlocking exisitingAccount
                }
                crypto.encode(privateKey)?.let { encodedPrivateKey ->
                    val newAccount = ZilAccountEntity(
                        encryptedPrivateKey = encodedPrivateKey,
                        name = accountName,
                        address = address,
                        publicKey = publicKey,
                        nonce = 0,
                        zilBalance = 0.0,
                        transactions = emptyList()
                    )
                    wallet.addAccount(newAccount)
                    if (wallet.activeAccountAddress == null) {
                        wallet.activeAccountAddress = newAccount.address
                    }
                    zilWalletDao().update(wallet)
                    return@runBlocking newAccount
                }
            }
            return@runBlocking null
        }
    }

// endregion

// region CONTACTS

    fun contactDelete(contact: ZilContactEntity) {
        runBlocking {
            val existingContact = zilContactDao().findByAddress(contact.address)
            if (existingContact != null) { // new wallet
                zilContactDao().delete(contact)
            }
        }
    }

    fun contactSave(contact: ZilContactEntity) {
        runBlocking {
            val existingContact = zilContactDao().findByAddress(contact.address)
            if (existingContact == null) { // new wallet
                zilContactDao().insertAll(contact)
            } else {
                zilContactDao().update(contact)
            }
        }
    }
// endregion
}


