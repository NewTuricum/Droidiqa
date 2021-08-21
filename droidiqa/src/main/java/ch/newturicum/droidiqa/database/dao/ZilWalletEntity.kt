package ch.newturicum.droidiqa.database.dao

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import ch.newturicum.droidiqa.dto.ZilAccount
import ch.newturicum.droidiqa.dto.ZilWallet
import ch.newturicum.droidiqa.network.ZilNetwork
import ch.newturicum.droidiqa.util.DroidiqaUtils

internal data class AccountContainer(
    var entries: MutableList<ZilAccountEntity>
)

internal data class TokenContainer(
    var entries: MutableList<ZilTokenEntity>
)

@Entity
internal data class ZilWalletEntity(
    @PrimaryKey val network: ZilNetwork, // MAIN or TEST
    @ColumnInfo(name = "activeAccountAddress") var activeAccountAddress: String?,
    @ColumnInfo(name = "accounts") val _accountContainer: AccountContainer,
    @ColumnInfo(name = "tokens") val _tokenContainer: TokenContainer,
) {

    fun toZilWallet(): ZilWallet {
        return ZilWallet(
            network,
            activeAccount()?.toZilAccount(),
            accounts().map { it.toZilAccount() },
            tokens().map { it.toZilToken() }
        )
    }

    fun accounts(): List<ZilAccountEntity> = _accountContainer.entries.toList()
    fun tokens(): List<ZilTokenEntity> = _tokenContainer.entries.toList()
    fun accountCount(): Int = _accountContainer.entries.size
    fun tokenCount(): Int = _tokenContainer.entries.size
    fun containsAccount(account: ZilAccountEntity): Boolean =
        _accountContainer.entries.contains(account)

    fun containsAccount(accountAddress: String): Boolean =
        _accountContainer.entries.find { it.address == accountAddress } != null

    fun findAccount(accountAddress: String): ZilAccountEntity? =
        accounts().find { it.address == accountAddress.trim() }

    fun containsToken(token: ZilTokenEntity): Boolean = _tokenContainer.entries.contains(token)
    fun containsToken(tokenAddress: String): Boolean =
        _tokenContainer.entries.find { tokenAddress == it.contractAddress } != null

    fun activeAccount(): ZilAccountEntity? {
        return when {
            accounts().isEmpty() -> null
            accounts().size == 1 -> accounts().first()
            else -> accounts().find { it.address == activeAccountAddress }
        }
    }

    fun getZilBalance(): Double =
        DroidiqaUtils.qaToZil((activeAccount()?.zilBalance ?: 0.0).toLong())

    fun getTokenBalance(token: ZilTokenEntity): Double {
        activeAccount()?.let {
            return token.getBalanceForAccount(it)
        }
        return 0.0
    }

    internal fun setAccounts(newAccounts: List<ZilAccountEntity>) {
        _accountContainer.entries = newAccounts.toMutableList()
    }

    internal fun setTokens(newTokens: List<ZilTokenEntity>) {
        _tokenContainer.entries = newTokens.toMutableList()
    }


    internal fun addToken(token: ZilTokenEntity): Boolean {
        if (!token.isValid()) {
            return false
        }
        _tokenContainer.entries.apply {
            if (!contains(token)) {
                add(token)
                sort()
            }
        }
        return true
    }

    internal fun removeToken(token: ZilTokenEntity) {
        _tokenContainer.entries.apply {
            if (contains(token)) {
                remove(token)
            }
        }
    }

    internal fun addAccount(account: ZilAccountEntity) {
        _accountContainer.entries.apply {
            if (!contains(account)) {
                add(account)
                sort()
            }
        }
    }

    internal fun removeAccount(account: ZilAccount) {
        _accountContainer.entries =
            accounts().filter { it.address != account.address }.toMutableList()
    }
}

