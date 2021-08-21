package ch.newturicum.droidiqa.repository

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.OnLifecycleEvent
import ch.newturicum.droidiqa.Constants
import ch.newturicum.droidiqa.dto.*
import ch.newturicum.droidiqa.dto.response.ZilSmartContractInit
import ch.newturicum.droidiqa.network.ZilNetwork
import ch.newturicum.droidiqa.transitions.Transition

interface DroidiqaRepository {

    /**
     * Set the network to be used. Will automatically switch the current wallet accordingly.
     * @param network The network to switch to.
     */
    fun setNetwork(network: ZilNetwork)

    /**
     * Set delay for automatic transaction status refresh calls. This will only be executed while
     * pending transactions are present. Default is disabled.
     * @param delayMs The delay in between refresh calls in milliseconds. Values below 1000ms will be ignored. A value of 0 disables automatic refreshes.
     */
    fun setTransactionRefreshInterval(delayMs: Long = Constants.DEFAULT_TRANSACTION_REFRESH_INTERVAL)

    /**
     * Set delay for automatic account balance refresh calls. Default is disabled.
     * @param delayMs The delay in between refresh calls in milliseconds. Values below 3000ms will be ignored. A value of 0 disables automatic refreshes.
     */
    fun setBalanceRefreshInterval(delayMs: Long = Constants.DEFAULT_BALANCE_REFRESH_INTERVAL)

    /**
     * Get the currently active network.
     * @return The network currently being used by the instance.
     */
    fun getNetwork(): ZilNetwork

    /**
     * Get the latest known state of the wallet.
     * @return An instance of ZilWallet or null.
     */
    fun getWallet(): ZilWallet?

    /**
     * Get the latest known list of tokens within the current wallet.
     * @return A list of ZilWallet or null.
     */
    fun getTokens(): List<ZilToken>?

    /**
     * Get the latest known list of accounts within the current wallet.
     * @return A list of ZilToken or null.
     */
    fun getAccounts(): List<ZilAccount>?

    /**
     * Get the active account within the current wallet.
     * @return An instance of ZilAccount or null.
     */
    fun getActiveAccount(): ZilAccount?

    /**
     * Set the active account within the current wallet.
     * @return <code>true</code> if the account was set successfully, <code>false</code> else.
     */
    fun setActiveAccount(account: ZilAccount): Boolean

    /**
     * @return The number of accounts in the currently selected wallet.
     */
    fun getAccountCount(): Int

    /**
     * Get the last known minimum gas price in Qa denomination.
     * @return The last known minimum gas price in Qa. Defaults to 0.
     */
    fun getMinGasPriceQa(): Long

    /**
     * Get the last known minimum gas price in Zil denomination.
     * @return The last known minimum gas price in Zil. Defaults to 0.0
     */
    fun getMinGasPriceZil(): Double

    /**
     * Add a token to the current wallet.
     * @param token The token to add.
     * @return <code>true</code> if the token was added successfully or is already present within the
     * wallet. <code>false</code> otherwise.
     */
    fun addToken(token: ZilToken): Boolean

    /**
     * Add a token to the current wallet.
     * @param smartContractInit The SmartContractInit object describing the token to add.
     * @return <code>true</code> if the token was added successfully or is already present within the
     * wallet. <code>false</code> otherwise.
     */
    fun addToken(smartContractInit: ZilSmartContractInit): Boolean

    /**
     * Add a token to the current wallet.
     * @param contractAddress The smart contract address of the token to add.
     * @param addTokenCallback An optional callback for receiving any results and errors.
     */
    fun addToken(contractAddress: String, addTokenCallback: AddTokenCallback? = null)

    /**
     * Find a token within the current wallet by its contract address.
     * @param contractAddress The smart contract address of the token.
     * @return The Ziltoken instance associated with contractAddress or null if the token is unknown.
     */
    fun findToken(contractAddress: String): ZilToken?

    /**
     * Remove a token from the current wallet.
     * @param token The token to remove.
     */
    fun removeToken(token: ZilToken)

    /**
     * Create a new account and add it to the current wallet.
     * @param name Optional. A human readable denominator for the new account.
     * @return The newly created account or <code>null</code> if an error occurred.
     */
    fun createAccount(name: String? = null): ZilAccount?

    /**
     * Add a new account to the current wallet.
     * @param privateKey The account's private key.
     * @param name Optional. A human readable denominator of the account.
     */
    fun addAccount(privateKey: String, name: String? = null): Boolean

    /**
     * Remove an exisiting account from the current wallet.
     * @param account The account to remove.
     */
    fun removeAccount(account: ZilAccount)

    /**
     * Update an existing account's data.
     * @param account The account object containing the new values.
     */
    fun updateAccount(account: ZilAccount)

    /**
     * Obtain the decoded raw private key of the given account.
     * @param account The account of which the private key should be decoded.
     * @return The decoded private key in raw format or null if the account cannot be found within the active wallet.
     */
    fun getPrivateKey(account: ZilAccount): String?

    /**
     * Triggers an async refresh of the current wallet's active account from the Zilliqa blockchain.
     * @param callback Optional. Callback triggered once the account's stats were updated from the Zilliqa blockchain.
     */
    fun refreshActiveAccount(callback: RefreshCallback? = null)

    /**
     * Triggers an async refresh of the current minimum gas price from the Zilliqa blockchain.
     * @param callback Optional. A receiver callback for results / errors.
     */
    fun refreshMinimumGasPrice(callback: MinimumGasPriceCallback? = null)

    /**
     * Add a new contact to the database.
     * @param zilContact The contact to add. Existing contact of identical address will be replaced.
     */
    fun addContact(zilContact: ZilContact)

    /**
     * Remove an existing contact from the database.
     * @param zilContact The contact to remove.
     */
    fun deleteContact(zilContact: ZilContact)

    /**
     * Find a contact by address.
     * @param address The contact's Zilliqa address.
     * @return A ZilContact instance of the contact associated with address, or null if the contact is unknown.
     */
    fun findContact(address: String): ZilContact?

    /**
     * Obtain a smart contract's init data from the Zilliqa blockchain.
     * @param contractAddress The smart contract's address.
     * @param callback A receiver for the result or errors.
     */
    fun getSmartContractInit(
        contractAddress: String,
        callback: SmartContractInitCallback?
    )

    /**
     * Obtain a smart contract's code from the Zilliqa blockchain.
     * @param contractAddress The smart contract's address.
     * @param callback A receiver for the result or errors.
     */
    fun getSmartContractCode(
        contractAddress: String,
        callback: SmartContractCodeCallback?
    )

    /**
     * Load the current wallet's active account's known transactions.
     * @return A list of transactions.
     */
    fun getTransactions(): List<ZilTransaction>

    /**
     * Add a transaction to the local database.
     * @param transaction The transaction to add. If the hash is already known, this does nothing.
     */
    fun addTransaction(transaction: ZilTransaction)

    /**
     * Remove a transaction from the local database.
     * @param transaction The transaction to remove.
     */
    fun deleteTransaction(transaction: ZilTransaction)

    /**
     * Look up a certain transaction hash in the local database.
     * @param hash The transaction's network hash.
     * @return A ZilTransaction object of the transaction identified by hash or null if the transaction hash is unknown.
     */
    fun findTransactionByHash(hash: String): ZilTransaction?

    /**
     * Trigger a refresh of all pending transaction stati.
     * @param callback Optional. A callback to be notified once the refresh is finished.
     */
    fun refreshPendingTransactions(callback: RefreshCallback? = null)

    /**
     * Send a Zilliqa tokens from the current wallet's active account.
     * @param amount The amount to send in Zil-denomination.
     * @param receiverAddress The target address where the Zilliqa tokens will be sent to.
     * @param gasPrice The gas price to use for the transaction.
     * @param callback Optional. A callback receiving the result or errors.
     */
    fun sendZilliqa(
        amount: Double,
        receiverAddress: String,
        gasPrice: Long,
        callback: TransactionCallback?
    )

    /**
     * Execute a smart contract transition.
     * @param contractAddress The smart contract's address.
     * @param gasPrice The gas price to use for the transaction.
     * @param transition The transition with the contract to call.
     * @param callback Optional. A callback receiving the result or errors.
     */
    fun callSmartContractTransition(
        contractAddress: String,
        gasPrice: Long,
        transition: Transition,
        callback: TransactionCallback?
    )

    /**
     * Shut down the repository manually. NOTE:
     * If your application class implements the AndroidX ProcessLifecycleOwner interface this will
     * be called automatically once the app is terminated.
     */
    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    fun shutdown()

}